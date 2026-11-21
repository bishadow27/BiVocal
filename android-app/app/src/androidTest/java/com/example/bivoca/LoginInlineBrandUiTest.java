package com.example.bivoca;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.example.bivoca.ui.auth.DangKyActivity;
import static org.junit.Assert.*;
/** Renders the actual Login XML on a native test host; preserves the user's authenticated session. No auth requests. */
@RunWith(AndroidJUnit4.class)
public class LoginInlineBrandUiTest {
 @Test public void headerAndGoogleColorsMatchRequestedPresentation()throws Exception{
  try(ActivityScenario<DangKyActivity> scenario=ActivityScenario.launch(DangKyActivity.class)){
   scenario.onActivity(a->{a.setContentView(R.layout.activity_dang_nhap);View logo=a.findViewById(R.id.loginMascot),title=a.findViewById(R.id.loginHeaderTitle),divider=a.findViewById(R.id.loginHeaderDivider);assertSame(logo.getParent(),title.getParent());assertSame(logo.getParent(),divider.getParent());assertEquals(View.VISIBLE,divider.getVisibility());assertEquals(View.GONE,a.findViewById(R.id.loginSubtitle).getVisibility());assertEquals("Đăng nhập",((TextView)title).getText().toString());MaterialButton google=a.findViewById(R.id.buttonGoogleSignIn);assertEquals(0xFF4285F4,google.getBackgroundTintList().getDefaultColor());assertEquals(0xFF1F1F1F,google.getCurrentTextColor());assertEquals(0xFF4285F4,google.getStrokeColor().getDefaultColor());assertNull(google.getIconTint());});
   LightColorfulUiTest.shot("login-inline-brand-native-xml.png");
  }
 }
}

