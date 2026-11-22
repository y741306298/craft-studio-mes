package com.mes.application.command.graphics;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** SVG planning still needs text in MES; never read an unbounded remote file into the MES heap. */
public final class SvgResourceReader {
    private static final int MAXIMUM=1048576;
    private static final RestTemplate HTTP;
    static{var factory=new SimpleClientHttpRequestFactory();factory.setConnectTimeout(Duration.ofSeconds(3));factory.setReadTimeout(Duration.ofSeconds(10));HTTP=new RestTemplate(factory);}
    private SvgResourceReader(){}
    public static byte[] read(URI uri){
        requireHttp(uri);
        return HTTP.execute(uri,HttpMethod.GET,null,response->{
            long size=response.getHeaders().getContentLength();if(size>MAXIMUM)throw new IllegalArgumentException("SVG 文件超过 MES 1MiB 上限");
            byte[] bytes=response.getBody().readNBytes(MAXIMUM+1);if(bytes.length>MAXIMUM)throw new IllegalArgumentException("SVG 文件超过 MES 1MiB 上限");return bytes;
        });
    }
    public static byte[] read(String url){return read(URI.create(url));}
    public static String text(String url){return new String(read(url),StandardCharsets.UTF_8);}

    /** Lets callers inspect XML geometry without buffering the whole SVG. */
    public static <T> T scanXml(URI uri, XmlScanner<T> scanner) {
        requireHttp(uri);
        return HTTP.execute(uri, HttpMethod.GET, null, response -> {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            try {
                XMLStreamReader xml = factory.createXMLStreamReader(response.getBody());
                try {
                    return scanner.scan(xml);
                } finally {
                    xml.close();
                }
            } catch (XMLStreamException e) {
                throw new IOException("解析 SVG 失败", e);
            }
        });
    }

    private static void requireHttp(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("SVG 来源必须是 HTTP(S) URL");
        }
    }

    @FunctionalInterface
    public interface XmlScanner<T> {
        T scan(XMLStreamReader xml) throws XMLStreamException;
    }
}
