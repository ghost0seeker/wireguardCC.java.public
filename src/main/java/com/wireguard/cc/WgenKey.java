package com.wireguard.cc;

import java.security.KeyPair;
import java.util.Base64;
import br.eti.balena.security.ecdh.curve25519.Curve25519KeyPairGenerator;

public class WgenKey {

    public String peerPrivateKey64;
    public String peerPublicKey64;

    public WgenKey () {

        Curve25519KeyPairGenerator keyPairGenerator = new Curve25519KeyPairGenerator();

        KeyPair peerKP = keyPairGenerator.generateKeyPair();

        byte[] peerPublicKey = peerKP.getPublic().getEncoded();

        byte[] peerPrivateKey = peerKP.getPrivate().getEncoded();

        this.peerPublicKey64 = Base64.getEncoder().encodeToString(peerPublicKey);
        //System.out.println("Public Key: " + peerPublicKey64);

        this.peerPrivateKey64 = Base64.getEncoder().encodeToString(peerPrivateKey);
        //System.out.println("Private Key: " + peerPrivateKey64);

        System.out.println("wireguard genkey initialised...");
    }
}
