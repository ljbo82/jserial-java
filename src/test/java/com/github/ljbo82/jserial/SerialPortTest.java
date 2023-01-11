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

import com.github.ljbo82.jserial.comm.SerialPortConnection;
import com.github.ljbo82.jserial.test.MessageConnection;
import com.github.ljbo82.jserial.test.Mode;
import com.github.ljbo82.jserial.test.PacketConnection;
import com.github.ljbo82.jserial.SerialPort.DataBits;
import com.github.ljbo82.jserial.SerialPort.Parity;
import com.github.ljbo82.jserial.SerialPort.StopBits;

import java.io.IOException;

public class SerialPortTest {

    public static void main(String[] args) throws IOException {
        String[] ports = SerialPort.getPortNames();
        if (ports.length == 0)
            throw new RuntimeException("No ports detected");

        if (ports.length > 1)
            System.out.println("Available serial ports:\n");

        String portName = ports[ports.length == 1 ? 0 : TestUtils.getOptionIndex("Choose a port: ", ports)];
        System.out.printf("Opening port %s... ", portName);
        System.out.flush();
        try (SerialPortConnection serialConnection = new SerialPortConnection(portName)) {
            serialConnection.config(9600, DataBits.DATA_BITS_8, Parity.NONE, StopBits.STOP_BITS_1);
            System.out.println("DONE!");

            MessageConnection messageConnection = new MessageConnection(serialConnection);
            System.out.print("[MSG] PING test... "); System.out.flush();
            messageConnection.ping("hello");
            System.out.println("DONE!");
            System.out.print("[MSG] PROT test... "); System.out.flush();
            messageConnection.protocol(9600, DataBits.DATA_BITS_8, Parity.NONE, StopBits.STOP_BITS_1, Mode.PACKET);
            System.out.println("DONE!");

            PacketConnection packetConnection = new PacketConnection(serialConnection);
            System.out.print("[PKT] PING test... "); System.out.flush();
            packetConnection.ping("world!");
            System.out.println("DONE!");
            System.out.print("[PKT] PROT test... "); System.out.flush();
            packetConnection.protocol(2400, DataBits.DATA_BITS_7, Parity.EVEN, StopBits.STOP_BITS_2, Mode.MESSAGE);
            System.out.println("DONE!");
            System.out.print("Protocol change test... "); System.out.flush();
            serialConnection.config(2400, DataBits.DATA_BITS_7, Parity.EVEN, StopBits.STOP_BITS_2);
            messageConnection.ping("HELLO");
            System.out.println("DONE!");
            System.out.print("Restoring default protocol... "); System.out.flush();
            messageConnection.protocol(9600, DataBits.DATA_BITS_8, Parity.NONE, StopBits.STOP_BITS_1, Mode.MESSAGE);
            System.out.println("DONE!");
        }
        System.out.println("SUCCESS!");
    }
}
