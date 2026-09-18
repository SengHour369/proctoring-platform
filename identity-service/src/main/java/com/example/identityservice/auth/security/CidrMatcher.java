package com.example.identityservice.auth.security;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** IPv4 CIDR containment check for {@code ApiClient.allowedIpRanges}. */
public final class CidrMatcher {

    private CidrMatcher() {
    }

    public static boolean matches(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            InetAddress rangeAddress = InetAddress.getByName(parts[0]);
            int prefixLength = parts.length > 1 ? Integer.parseInt(parts[1]) : 32;

            byte[] rangeBytes = rangeAddress.getAddress();
            byte[] ipBytes = InetAddress.getByName(ip).getAddress();
            if (rangeBytes.length != ipBytes.length) {
                return false;
            }

            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (rangeBytes[i] != ipBytes[i]) {
                    return false;
                }
            }
            if (remainingBits > 0) {
                int mask = 0xFF << (8 - remainingBits);
                return (rangeBytes[fullBytes] & mask) == (ipBytes[fullBytes] & mask);
            }
            return true;
        } catch (UnknownHostException | ArrayIndexOutOfBoundsException | NumberFormatException e) {
            return false;
        }
    }
}
