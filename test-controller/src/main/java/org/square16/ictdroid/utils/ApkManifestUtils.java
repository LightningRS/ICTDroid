package org.square16.ictdroid.utils;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

@Slf4j
public class ApkManifestUtils {
    private static Node findCompNode(Document apkManifest, String packageName, String compName) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            NodeList activityNodesFull = (NodeList) xPath.evaluate(
                    "//*[@name='" + compName + "']",
                    apkManifest, XPathConstants.NODESET);
            NodeList activityNodesShort = (NodeList) xPath.evaluate(
                    "//*[@name='" + compName.replaceFirst(packageName, "") + "']",
                    apkManifest, XPathConstants.NODESET);
            NodeList activityNodes = activityNodesFull.getLength() > 0 ? activityNodesFull : activityNodesShort;

            if (activityNodes.getLength() < 1) {
                return null;
            }
            return activityNodes.item(0);
        } catch (Exception e) {
            log.warn("{} occurred when finding component [{}/{}] in AndroidManifest.xml!",
                    e.getClass().getSimpleName(), packageName, compName, e);
            return null;
        }
    }

    public static void checkCompExported(CompModel compModel, Document apkManifest) {
        boolean isExported = false;
        Node compNode = findCompNode(apkManifest, compModel.getPackageName(), compModel.getClassName());
        if (compNode == null) {
            return;
        }
        NodeList intentFilterNodes = ((Element) compNode).getElementsByTagName("intent-filter");
        if (intentFilterNodes.getLength() > 0) {
            isExported = true;
        }
        Node exportedAttr = compNode.getAttributes().getNamedItem("android:exported");
        if (!isExported && exportedAttr != null) {
            isExported = "true".equals(exportedAttr.getTextContent());
        }
        compModel.setExported(isExported);
    }

    public static void checkCompEnabled(CompModel compModel, Document apkManifest) {
        Node compNode = findCompNode(apkManifest, compModel.getPackageName(), compModel.getClassName());
        if (compNode == null) {
            return;
        }
        Node enabledAttr = compNode.getAttributes().getNamedItem("android:enabled");
        compModel.setEnabled(enabledAttr == null || "true".equals(enabledAttr.getTextContent()));
    }
}
