package com.mes.application.command.typesetting.service;

import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import org.xml.sax.InputSource;

/** 算法可能将正面 mask 的 mark 一起复制到反面；保留图片矩形与排版变换，去掉工艺标记。 */
public final class SublimationMirrorSvgCleaner {
    private SublimationMirrorSvgCleaner() { }

    public static String clean(String svg) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(svg)));
            var groups = document.getElementsByTagNameNS("*", "g");
            boolean changed = false;
            for (int i = groups.getLength() - 1; i >= 0; i--) {
                Element group = (Element) groups.item(i);
                String id = group.getAttribute("id");
                if (id.startsWith("sublimation-") && !id.startsWith("sublimation-original-")) {
                    group.getParentNode().removeChild(group);
                    changed = true;
                }
            }
            if (!changed) return svg;
            TransformerFactory transformers = TransformerFactory.newInstance();
            transformers.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformers.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            var transformer = transformers.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter output = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(output));
            return output.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("热升华反面 SVG 无法清理 mark", e);
        }
    }
}
