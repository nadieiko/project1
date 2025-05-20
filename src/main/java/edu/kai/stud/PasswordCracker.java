package edu.kai.stud;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.math.BigInteger;

public class PasswordCracker {
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final String CYRILLIC_LOWER = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя";
    private static final String CYRILLIC_UPPER = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ";
    
    private static final int BATCH_SIZE = 200000;
    private static final int QUEUE_SIZE = 32;
    private static final int MIN_BATCH_SIZE = 10000;
    private static final int PASSWORD_VERIFICATION_BATCH = 1000;

    private final SecurityManager securityManager;
    private final int threadCount;
    private final AtomicBoolean passwordFound;
    private final AtomicLong combinationsTried;
    private String foundPassword;
    private volatile boolean shouldStop;
    private final ThreadLocal<char[]> bufferCache;
    private final ForkJoinPool executor;
    private String cachedCharset;
    private char[] cachedCharsetArray;
    private int cachedCharsetSize;

    public enum CrackingScenario {
        NO_INFO("Немає інформації про пароль"),
        EXACT_LENGTH("Відома точна довжина пароля"),
        APPROXIMATE_LENGTH("Відома приблизна довжина пароля (±1)"),
        EXACT_LENGTH_AND_CHARSET("Відома точна довжина та набори символів"),
        APPROXIMATE_LENGTH_AND_CHARSET("Відома приблизна довжина (±1) та набори символів");

        private final String description;

        CrackingScenario(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public PasswordCracker(SecurityManager securityManager) {
        this.securityManager = securityManager;
        this.threadCount = Runtime.getRuntime().availableProcessors();
        this.passwordFound = new AtomicBoolean(false);
        this.combinationsTried = new AtomicLong(0);
        this.bufferCache = ThreadLocal.withInitial(() -> new char[20]);
        
        this.executor = new ForkJoinPool(
            threadCount,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            (t, e) -> { e.printStackTrace(); },
            true,
            threadCount,
            threadCount * 8,
            1,
            null,
            60, TimeUnit.SECONDS
        );
    }

    private void initializeCharset(String charset) {
        if (!charset.equals(cachedCharset)) {
            cachedCharset = charset;
            cachedCharsetArray = charset.toCharArray();
            cachedCharsetSize = charset.length();
        }
    }

    public static class PasswordConstraints {
        private final CrackingScenario scenario;
        private final int exactLength;
        private final int approximateLength;
        private final boolean useLowercase;
        private final boolean useUppercase;
        private final boolean useDigits;
        private final boolean useSpecial;
        private final boolean useCyrillicLower;
        private final boolean useCyrillicUpper;

        public PasswordConstraints(CrackingScenario scenario, int length,
                                 boolean useLowercase, boolean useUppercase,
                                 boolean useDigits, boolean useSpecial,
                                 boolean useCyrillicLower, boolean useCyrillicUpper) {
            this.scenario = scenario;
            this.exactLength = length;
            this.approximateLength = length;
            this.useLowercase = useLowercase;
            this.useUppercase = useUppercase;
            this.useDigits = useDigits;
            this.useSpecial = useSpecial;
            this.useCyrillicLower = useCyrillicLower;
            this.useCyrillicUpper = useCyrillicUpper;
        }

        public String getCharacterSet() {
            StringBuilder charset = new StringBuilder();
            if (scenario == CrackingScenario.NO_INFO || 
                scenario == CrackingScenario.EXACT_LENGTH || 
                scenario == CrackingScenario.APPROXIMATE_LENGTH) {
                // Якщо немає інформації про набори символів, використовуємо всі
                charset.append(LOWERCASE).append(UPPERCASE)
                       .append(DIGITS).append(SPECIAL)
                       .append(CYRILLIC_LOWER).append(CYRILLIC_UPPER);
            } else {
                // Використовуємо тільки вказані набори
                if (useLowercase) charset.append(LOWERCASE);
                if (useUppercase) charset.append(UPPERCASE);
                if (useDigits) charset.append(DIGITS);
                if (useSpecial) charset.append(SPECIAL);
                if (useCyrillicLower) charset.append(CYRILLIC_LOWER);
                if (useCyrillicUpper) charset.append(CYRILLIC_UPPER);
            }
            return charset.toString();
        }

        public int getMinLength() {
            switch (scenario) {
                case NO_INFO:
                    return 1;
                case APPROXIMATE_LENGTH:
                case APPROXIMATE_LENGTH_AND_CHARSET:
                    return Math.max(1, approximateLength - 1);
                case EXACT_LENGTH:
                case EXACT_LENGTH_AND_CHARSET:
                    return exactLength;
                default:
                    return 1;
            }
        }

        public int getMaxLength() {
            switch (scenario) {
                case NO_INFO:
                    return 8;
                case APPROXIMATE_LENGTH:
                case APPROXIMATE_LENGTH_AND_CHARSET:
                    return approximateLength + 1;
                case EXACT_LENGTH:
                case EXACT_LENGTH_AND_CHARSET:
                    return exactLength;
                default:
                    return 8;
            }
        }
    }

    // Додаємо геттер для відстеження прогресу
    public long getCombinationsTried() {
        return combinationsTried.get();
    }

    private class PasswordCrackerTask implements Callable<String> {
        private final String username;
        private final int length;
        private final long startIndex;
        private final long endIndex;
        private final char[] buffer;
        private final StringBuilder passwordBuilder;
        private final List<String> passwordBatch;

        public PasswordCrackerTask(String username, int length,
                                 long startIndex, long endIndex) {
            this.username = username;
            this.length = length;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.buffer = bufferCache.get();
            this.passwordBuilder = new StringBuilder(length);
            this.passwordBatch = new ArrayList<>(PASSWORD_VERIFICATION_BATCH);
        }

        @Override
        public String call() {
            try {
                long localCounter = 0;
                
                for (long i = startIndex; i < endIndex && !shouldStop; i++) {
                    generatePassword(i, buffer, length);
                    
                    passwordBuilder.setLength(0);
                    passwordBuilder.append(buffer, 0, length);
                    String attempt = passwordBuilder.toString();
                    passwordBatch.add(attempt);

                    localCounter++;
                    if (localCounter >= PASSWORD_VERIFICATION_BATCH) {
                        // Перевіряємо пакет паролів
                        for (String pwd : passwordBatch) {
                            if (securityManager.validatePassword(username, pwd)) {
                                combinationsTried.addAndGet(localCounter);
                                foundPassword = pwd;
                                passwordFound.set(true);
                                return pwd;
                            }
                        }
                        
                        combinationsTried.addAndGet(localCounter);
                        localCounter = 0;
                        passwordBatch.clear();
                    }
                }
                
                // Перевіряємо залишок
                if (!passwordBatch.isEmpty()) {
                    for (String pwd : passwordBatch) {
                        if (securityManager.validatePassword(username, pwd)) {
                            combinationsTried.addAndGet(localCounter);
                            foundPassword = pwd;
                            passwordFound.set(true);
                            return pwd;
                        }
                    }
                    combinationsTried.addAndGet(localCounter);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void generatePassword(long index, char[] buffer, int length) {
        for (int pos = length - 1; pos >= 0; pos--) {
            buffer[pos] = cachedCharsetArray[(int)(index % cachedCharsetSize)];
            index /= cachedCharsetSize;
        }
    }

    public void stopCracking() {
        shouldStop = true;
    }

    public CrackingResult crackPassword(String username, PasswordConstraints constraints) {
        long startTime = System.currentTimeMillis();
        shouldStop = false;
        passwordFound.set(false);
        combinationsTried.set(0);

        try {
            for (int length = constraints.getMinLength(); 
                 length <= constraints.getMaxLength() && !shouldStop && !passwordFound.get(); 
                 length++) {
                
                String charset = constraints.getCharacterSet();
                crackWithCharset(username, length, charset);
                
                if (passwordFound.get()) break;
            }
        } finally {
            shouldStop = true;
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        return new CrackingResult(foundPassword, duration, combinationsTried.get(),
            calculateTotalCombinations(cachedCharsetSize, 
                constraints.getMinLength(), constraints.getMaxLength()));
    }

    private void crackWithCharset(String username, int length, String charset) {
        initializeCharset(charset);
        
        long totalCombinationsForLength = BigInteger.valueOf(cachedCharsetSize)
            .pow(length).longValue();
        
        // Адаптивний розмір пакету
        int actualBatchSize = Math.max(MIN_BATCH_SIZE, 
            (int)Math.min(BATCH_SIZE, totalCombinationsForLength / (threadCount * 4)));
        
        CompletionService<String> completionService = 
            new ExecutorCompletionService<>(executor);
        
        // Запускаємо генератор завдань
        long tasksSubmitted = 0;
        for (long start = 0; start < totalCombinationsForLength && !shouldStop; 
             start += actualBatchSize) {
            long end = Math.min(start + actualBatchSize, totalCombinationsForLength);
            
            completionService.submit(new PasswordCrackerTask(
                username, length, start, end
            ));
            tasksSubmitted++;
        }

        // Обробляємо результати
        for (long i = 0; i < tasksSubmitted && !shouldStop; i++) {
            try {
                Future<String> future = completionService.poll(50, TimeUnit.MILLISECONDS);
                if (future == null) continue;
                
                String result = future.get(50, TimeUnit.MILLISECONDS);
                if (result != null) {
                    foundPassword = result;
                    shouldStop = true;
                    break;
                }
            } catch (TimeoutException e) {
                continue;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private long calculateTotalCombinations(int charsetLength, int minLength, int maxLength) {
        BigInteger total = BigInteger.ZERO;
        for (int length = minLength; length <= maxLength; length++) {
            total = total.add(BigInteger.valueOf(charsetLength).pow(length));
        }
        return total.longValue();
    }

    public static class CrackingResult {
        private final String password;
        private final long duration;
        private final long combinationsTried;
        private final long totalCombinations;

        public CrackingResult(String password, long duration, long combinationsTried, long totalCombinations) {
            this.password = password;
            this.duration = duration;
            this.combinationsTried = combinationsTried;
            this.totalCombinations = totalCombinations;
        }

        public String getPassword() {
            return password;
        }

        public long getDuration() {
            return duration;
        }

        public long getCombinationsTried() {
            return combinationsTried;
        }

        public long getTotalCombinations() {
            return totalCombinations;
        }
    }
} 