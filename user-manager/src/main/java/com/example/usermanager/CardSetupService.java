package com.example.usermanager;

import javax.crypto.SecretKey;
import javax.smartcardio.*;
import java.util.List;

public class CardSetupService {

    private static final byte[] APPLET_AID = {
        (byte) 0xA0, 0x00, 0x00, 0x00, 0x62, 0x03, 0x01, 0x0A, 0x03
    };
    private static final byte INS_SET_ID = (byte) 0x01;
    private static final byte INS_SET_AES_KEY = (byte) 0x03;
    private static final byte INS_SET_PIN = (byte) 0x08;
    private static final byte CLA = (byte) 0x80;

    public void setupCard(String userId, byte[] keyBytes, String pin) throws Exception {
        Card card = initializeCard();
        if (card == null) {
            throw new RuntimeException("Cartão não encontrado.");
        }

        CardChannel channel = card.getBasicChannel();

        ResponseAPDU response = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, APPLET_AID));
        checkStatus(response, "Selecionar applet");

        response = channel.transmit(new CommandAPDU(CLA, INS_SET_AES_KEY, 0x00, 0x00, keyBytes));
        checkStatus(response, "Enviar AES Key");

        response = channel.transmit(new CommandAPDU(CLA, INS_SET_ID, 0x00, 0x00, userId.getBytes("UTF-8")));
        checkStatus(response, "Enviar ID");

        if (pin != null && !pin.isEmpty()) {
            response = channel.transmit(new CommandAPDU(CLA, INS_SET_PIN, 0x00, 0x00, pin.getBytes("UTF-8")));
            checkStatus(response, "Enviar PIN");
        }

        System.out.println("Setup do cartão concluído.");
        card.disconnect(false);
    }

    private Card initializeCard() throws Exception {
        TerminalFactory factory = TerminalFactory.getDefault();
        List<CardTerminal> terminals = factory.terminals().list();

        if (terminals.isEmpty()) {
            System.out.println("Nenhum leitor encontrado.");
            return null;
        }

        CardTerminal terminal = terminals.get(0);
        System.out.println("Por favor, insira o cartão...");
        terminal.waitForCardPresent(0);

        Card card = terminal.connect("*");
        System.out.println("Cartão conectado!");
        return card;
    }

    private void checkStatus(ResponseAPDU response, String op) {
        if (response.getSW() != 0x9000) {
            throw new RuntimeException(op + " falhou: " + Integer.toHexString(response.getSW()));
        } else {
            System.out.println(op + " OK");
        }
    }
}
