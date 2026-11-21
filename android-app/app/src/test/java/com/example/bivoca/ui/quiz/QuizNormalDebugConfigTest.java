package com.example.bivoca.ui.quiz;
import com.example.bivoca.BuildConfig;
import org.junit.Test;
import static org.junit.Assert.*;
public class QuizNormalDebugConfigTest {
 @Test public void normalDebugUsesV2WithoutE2eHostOverride(){
  assertEquals("com.example.bivoca",BuildConfig.APPLICATION_ID);
  assertTrue(BuildConfig.QUIZ_SESSION_V2_ENABLED);
  assertEquals("",BuildConfig.API_BASE_URL_OVERRIDE);
 }
}
