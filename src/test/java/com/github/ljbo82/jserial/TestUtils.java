/*
 * Copyright (c) 2023 Leandro José Britto de Oliveira
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.ljbo82.jserial;

import java.util.Scanner;

public class TestUtils {
    public static int getOptionIndex(String prompt, String... options) {
        Scanner scanner = new Scanner(System.in);

        for (int i = 0; i < options.length; i++) {
            System.out.printf("%d) %s\n", i, options[i]);
        }

        while (true) {
            try {
                System.out.printf(prompt);
                System.out.flush();
                int index = scanner.nextInt();
                if (index < 0 || index >= options.length) {
                    System.out.println("Invalid option");
                } else {
                    scanner.close();
                    return index;
                }
            } catch (RuntimeException e) {
                System.out.printf("Invalid option (%s)", e.getMessage());
            }
        }
    }
}
