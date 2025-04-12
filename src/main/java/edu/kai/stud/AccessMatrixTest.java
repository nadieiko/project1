package edu.kai.stud;

import java.util.HashMap;
import java.util.Map;

public class AccessMatrixTest {
    public static void main(String[] args) {
        AccessMatrix accessMatrix = new AccessMatrix();

        // Додавання користувачів
        accessMatrix.addUser("user1");
        accessMatrix.addUser("user2");

        // Додавання ресурсів з правами
        AccessMatrix.AccessRights textFileRights = new AccessMatrix.AccessRights(true, true, true, false, "08:00-18:00");
        AccessMatrix.AccessRights exeFileRights = new AccessMatrix.AccessRights(false, false, false, true, "09:00-17:00");

        accessMatrix.addResource("user1", "document.txt", textFileRights);
        accessMatrix.addResource("user1", "program.exe", exeFileRights);

        // Перевірка прав доступу
        System.out.println("User1 can view document.txt: " + accessMatrix.getAccessRights("user1", "document.txt").canView());
        System.out.println("User1 can execute program.exe: " + accessMatrix.getAccessRights("user1", "program.exe").canExecute());

        // Оновлення прав доступу
        AccessMatrix.AccessRights updatedRights = new AccessMatrix.AccessRights(true, false, false, false, "08:00-18:00");
        accessMatrix.updateAccessRights("user1", "document.txt", updatedRights);

        // Перевірка оновлених прав
        System.out.println("User1 can edit document.txt after update: " + accessMatrix.getAccessRights("user1", "document.txt").canEdit());
    }
} 