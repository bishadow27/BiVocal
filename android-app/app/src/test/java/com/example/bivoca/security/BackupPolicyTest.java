package com.example.bivoca.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Checks real packaged policies without reading any session preference values. */
public class BackupPolicyTest {
    @Test
    public void legacyBackupExcludesOnlySessionPreferences() throws Exception {
        assertOnlySessionExcluded(policy("backup_rules.xml").getDocumentElement());
    }

    @Test
    public void cloudBackupExcludesOnlySessionPreferences() throws Exception {
        assertOnlySessionExcluded(section("cloud-backup"));
    }

    @Test
    public void deviceTransferExcludesOnlySessionPreferences() throws Exception {
        assertOnlySessionExcluded(section("device-transfer"));
    }

    private static Element section(String name) throws Exception {
        NodeList sections = policy("data_extraction_rules.xml").getElementsByTagName(name);
        assertEquals("Expected exactly one " + name + " section", 1, sections.getLength());
        return (Element) sections.item(0);
    }

    private static void assertOnlySessionExcluded(Element section) {
        assertNotNull(section);
        int policyCount = 0;
        for (Node child = section.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            Element rule = (Element) child;
            policyCount++;
            assertEquals("Preserve backup of other data", "exclude", rule.getTagName());
            assertEquals("sharedpref", rule.getAttribute("domain"));
            assertEquals("bivoca_session.xml", rule.getAttribute("path"));
        }
        assertEquals("Exclude exactly the session preference file", 1, policyCount);
    }

    private static org.w3c.dom.Document policy(String filename) throws Exception {
        Path cwd = Paths.get(System.getProperty("user.dir"));
        String[] roots = {"src/main/res/xml", "app/src/main/res/xml", "android-app/app/src/main/res/xml"};
        for (String root : roots) {
            Path candidate = cwd.resolve(root).resolve(filename);
            if (Files.isRegularFile(candidate)) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setExpandEntityReferences(false);
                return factory.newDocumentBuilder().parse(candidate.toFile());
            }
        }
        throw new AssertionError("Cannot locate packaged XML policy " + filename);
    }
}
