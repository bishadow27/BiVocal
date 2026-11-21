package com.example.bivoca.ui.quiz;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
public class QuizTextContrastTest {
 private Path main(){return Paths.get("src/main");}
 private String value(String name)throws Exception{for(String file:new String[]{"quiz_session.xml","light_colorful_tokens.xml","brand_refresh.xml"}){Document d=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(main().resolve("res/values/"+file).toFile());NodeList colors=d.getElementsByTagName("color");for(int i=0;i<colors.getLength();i++){Element e=(Element)colors.item(i);if(name.equals(e.getAttribute("name"))){String v=e.getTextContent().trim();return v.startsWith("@color/")?value(v.substring(7)):v;}}}throw new AssertionError("Missing token "+name);}
 private double l(String color){int rgb=Integer.parseInt(color.substring(1),16);double[] c={((rgb>>16)&255)/255d,((rgb>>8)&255)/255d,(rgb&255)/255d};for(int i=0;i<3;i++)c[i]=c[i]<=.04045?c[i]/12.92:Math.pow((c[i]+.055)/1.055,2.4);return .2126*c[0]+.7152*c[1]+.0722*c[2];}
 @Test public void smallSecondaryTextMeetsContrastOnExistingSurfaces()throws Exception{double text=l(value("quiz_v2_secondary"));for(String background:new String[]{"quiz_v2_canvas","quiz_v2_surface","quiz_v2_soft"}){double bg=l(value(background));double ratio=(Math.max(text,bg)+.05)/(Math.min(text,bg)+.05);assertTrue(background+" contrast "+ratio,ratio>=4.5);}}
 @Test public void javaRendererUsesSameResourceAsXml()throws Exception{String java=Files.readString(main().resolve("java/com/example/bivoca/ui/quiz/QuizSessionScreen.java"));assertTrue(java.contains("SECONDARY=activity.getColor(R.color.quiz_v2_secondary)"));assertFalse(java.contains("SECONDARY=Color.rgb(113,128,144)"));}
 @Test public void refreshedBrandBodyAndCtaHaveReadableContrast()throws Exception{assertContrast(value("brand_ink"),value("brand_canvas"));assertContrast(value("brand_secondary"),value("brand_canvas"));assertContrast(value("brand_on_cta"),value("brand_cta"));assertContrast(value("brand_on_teal"),value("brand_teal_ink"));}
 @Test public void sixMethodDescriptionsStayReadableOnBothGradientStops()throws Exception{for(int i=0;i<6;i++){Document d=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(main().resolve("res/drawable/quiz_v2_method_"+i+".xml").toFile());Element g=(Element)d.getElementsByTagName("gradient").item(0);assertContrast("#FFFFFF",g.getAttribute("android:startColor"));assertContrast("#FFFFFF",g.getAttribute("android:endColor"));}}
 private void assertContrast(String foreground,String background){double f=l(foreground),b=l(background);double ratio=(Math.max(f,b)+.05)/(Math.min(f,b)+.05);assertTrue(foreground+" on "+background+" contrast "+ratio,ratio>=4.5);}
}