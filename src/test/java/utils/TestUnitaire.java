package utils;

import org.junit.Assert;

class DBTest {

    @org.junit.jupiter.api.Test
    void passwordStrong() {
        String testPassword= "123Azerty@123";
        String test2= "123";
        Assert.assertTrue(DB.passwordStrong(testPassword));
        Assert.assertFalse(DB.passwordStrong(test2));
    }

    @org.junit.jupiter.api.Test
    void verifMail() {
        String mailTest="test@test.com";
        Assert.assertTrue(DB.verifMail(mailTest));
        String mailFail="testtest";
        Assert.assertFalse(DB.verifMail(mailFail));
    }
}