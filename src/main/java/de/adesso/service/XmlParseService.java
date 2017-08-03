package de.adesso.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * This class creates XML files.
 */
@Service
public class XmlParseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlParseService.class);

    @Value("${xml-output.path}")
    private String XML_OUTPUT_PATH;

    private PostParseService postParseService;

    @Autowired
    public XmlParseService(PostParseService postParseService) {
        this.postParseService = postParseService;
    }

    public XmlParseService() {
    }

    /**
     * generates XML files
     */
    public void generateXmlFiles() {
        Map<String, String> xmlContentsMap = postParseService.retrieveXmlContentFromHtmlFiles();
        for (Map.Entry<String, String> pair : xmlContentsMap.entrySet()) {
            String fileName = pair.getKey();
            String content = pair.getValue();
            try {
                Path pathToFile = Paths.get(fileName);

                Path file = Files.createDirectories(pathToFile.getParent());
                Files.write(pathToFile, content.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
