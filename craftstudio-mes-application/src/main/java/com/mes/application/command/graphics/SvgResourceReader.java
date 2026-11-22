package com.mes.application.command.graphics;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Reads complete SVG text for editing, or streams XML metadata for layout planning. */
public final class SvgResourceReader {
    private static final RestTemplate HTTP = createHttpClient();

    private SvgResourceReader() {
    }

    private static RestTemplate createHttpClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return new RestTemplate(factory);
    }

    public static byte[] read(String url) {
        return download(URI.create(url), response -> response.getBody().readAllBytes());
    }

    public static String text(String url) {
        return download(URI.create(url), response -> readText(response.getBody()));
    }

    public static String readText(InputStream input) throws IOException {
        return StreamUtils.copyToString(input, StandardCharsets.UTF_8);
    }

    public static <T> T scanXml(URI uri, XmlScanner<T> scanner) {
        return download(uri, response -> scanXml(response.getBody(), scanner));
    }

    public static <T> T scanXml(InputStream input, XmlScanner<T> scanner) throws IOException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try {
            XMLStreamReader xml = factory.createXMLStreamReader(input);
            try {
                return scanner.scan(xml);
            } finally {
                xml.close();
            }
        } catch (XMLStreamException e) {
            throw new IOException("解析 SVG 失败", e);
        }
    }

    private static <T> T download(URI uri, ResponseExtractor<T> extractor) {
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("SVG 来源必须是 HTTP(S) URL");
        }
        return HTTP.execute(uri, HttpMethod.GET, null, extractor);
    }

    @FunctionalInterface
    public interface XmlScanner<T> {
        T scan(XMLStreamReader xml) throws XMLStreamException;
    }
}
