package com.example.bivoca.data.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApiBaseUrlTest {

    @Test
    public void googleApiEmulatorUsesHostLoopbackAlias() {
        assertEquals(
                "http://10.0.2.2:3000/api/v1/",
                ApiBaseUrl.resolve(
                        "google/sdk_gphone64_x86_64/emu64xa:15/test-keys",
                        "sdk_gphone64_x86_64",
                        "Google",
                        "google",
                        "emu64xa",
                        "sdk_gphone64_x86_64"
                )
        );
    }

    @Test
    public void physicalDeviceKeepsAdbReverseAddress() {
        assertEquals(
                "http://127.0.0.1:3000/api/v1/",
                ApiBaseUrl.resolve(
                        "OPPO/CPH2557/OP5A51L1:15/release-keys",
                        "CPH2557",
                        "OPPO",
                        "OPPO",
                        "OP5A51L1",
                        "CPH2557"
                )
        );
    }
}
