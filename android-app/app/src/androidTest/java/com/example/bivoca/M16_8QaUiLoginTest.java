package com.example.bivoca;
import android.content.Intent;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.bivoca.data.api.RetrofitClient;
import com.example.bivoca.data.api.ApiBaseUrl;
import com.example.bivoca.ui.auth.DangNhapActivity;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
/** Only QA UI driver. No credentials, tokens or Auth storage are read or exported. */
@RunWith(AndroidJUnit4.class) public class M16_8QaUiLoginTest {
 @Test public void waitForOperatorSyntheticUiLogin() throws Exception {
  var context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  assertEquals("com.example.bivoca.e2e",context.getPackageName());
  assertEquals("http://10.0.2.2:3001/api/v1/",ApiBaseUrl.current());
  Field field=RetrofitClient.class.getDeclaredField("retrofit");field.setAccessible(true);
  // The test server deliberately runs slow two-layer guards before each QA login write.
  field.set(null,new Retrofit.Builder().baseUrl(ApiBaseUrl.current()).client(new OkHttpClient.Builder().readTimeout(90,TimeUnit.SECONDS).callTimeout(120,TimeUnit.SECONDS).build()).addConverterFactory(GsonConverterFactory.create()).build());
  ActivityScenario<DangNhapActivity> scenario=ActivityScenario.launch(new Intent(context,DangNhapActivity.class));
  try {
   // Host sends only the synthetic QA credential through a loopback ADB forward, RAM-only.
   try(java.net.ServerSocket listener=new java.net.ServerSocket(39187,1,java.net.InetAddress.getByName("127.0.0.1"))){listener.setSoTimeout(90000);try(java.net.Socket socket=listener.accept()){socket.setSoTimeout(5000);String line=new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream(),java.nio.charset.StandardCharsets.UTF_8)).readLine();assertNotNull(line);new java.io.PrintWriter(socket.getOutputStream(),true).println("RECEIVED");assertTrue(line.length()<1024);org.json.JSONObject qa=new org.json.JSONObject(line);assertEquals("qa_ft_a@fixture.invalid",qa.getString("email"));scenario.onActivity(a->{try{((android.widget.EditText)a.findViewById(R.id.editEmail)).setText(qa.getString("email"));((android.widget.EditText)a.findViewById(R.id.editPassword)).setText(qa.getString("password"));a.findViewById(R.id.buttonLogin).performClick();}catch(Exception e){throw new AssertionError("QA input failed");}});}}
   long until=SystemClock.elapsedRealtime()+120000;while(SystemClock.elapsedRealtime()<until){boolean[] home={false};InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{android.app.Activity a=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).stream().findFirst().orElse(null);home[0]=a instanceof com.example.bivoca.ui.home.TrangChuActivity;});if(home[0])return;SystemClock.sleep(300);}fail("Synthetic QA UI login did not reach Home; no secrets exported");}finally{scenario.close();}
 }
}