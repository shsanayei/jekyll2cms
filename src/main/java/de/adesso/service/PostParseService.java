package de.adesso.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * This class extracts the HTML data of a blog post and maps the information to Post and Image objects.
 */
@Service
public class PostParseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostParseService.class);

    /* The path where the generated jekyll files live. */
    @Value("${repository.local.htmlposts.path}")
    private String LOCAL_HTML_POSTS_PATH;

    @Value("${repository.local.path}")
    private String LOCAL_REPO_PATH;

    @Value("${xml-output.path}")
    private String XML_OUTPUT_PATH;

    @Autowired
    private RepoService repoService;

    /**
     * retrieves XML content from html files and creates a map having the file name
     * as key and the content as value.
     *
     * @return Map
     */
    public Map<String, String> retrieveXmlContentFromHtmlFiles() {
        Map<String, String> xmlContentsMap = new HashMap<>();
        List<File> htmlFiles = extractAllHtmlFilesFromDirectory();
        for (File f : htmlFiles) {
            String xmlPortion = extractXmlPortionFromHtmlFile(f, "_xml-content_");
            xmlContentsMap.put(generateXmlFileName(f), xmlPortion);
        }
        return xmlContentsMap;
    }

    /**
     * generates XML file name from the file path.
     *
     * @param filePath
     * @return String
     */
    private String generateXmlFileName(File filePath) {
        String dateFolderName = filePath.getParent().substring(filePath.getParent().lastIndexOf("\\") + 1);
        String fileName = filePath.getPath().substring(filePath.getPath().lastIndexOf("\\") + 1, filePath.getPath().lastIndexOf("."));
        return String.format("%s%s/%s-%s.xml", XML_OUTPUT_PATH, dateFolderName, dateFolderName, fileName);
    }

    /**
     * extracts XML portion from HTML file using a delimiter.
     *
     * @param htmlFile - html file
     * @param delimiter - delimiter
     * @return String
     */
    private String extractXmlPortionFromHtmlFile(File htmlFile, String delimiter) {
        String htmlFileContent = "";
        try {
            htmlFileContent = new String(Files.readAllBytes(Paths.get(htmlFile.getPath())), "UTF-8");

        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] splittedContent = htmlFileContent.split(delimiter);

        return splittedContent[1];
    }

    /**
     * TODO Optimization for case where file does not have extension
     * cut off the extension of file names.
     *
     * @param fileName - The file name of interest.
     * @return String
     */
    private String cutOffFileExtension(String fileName) {
        String fileNameWithoutExtension;
        if (fileName.contains(".")) {
            fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf("."));
        } else {
            fileNameWithoutExtension = fileName;
        }
        return fileNameWithoutExtension;
    }

    /**
     * Extracts the HTML post content of the HTML file.
     *
     * @param htmlFile The HTML post file
     * @return String - Post content as HTML code
     */
    private String extractHtmlPostContent(File htmlFile) {
        String method = "extractHtmlPostContent";
        Document doc = null;
        try {
            doc = Jsoup.parse(htmlFile, "UTF-8");
        } catch (IOException e) {
            LOGGER.error("In method {}: There was an error reading files. Error message: {}", method, e.getMessage());
            e.printStackTrace();
        }
        return doc.html();
    }

    /**
     * gets the first paragraph of the post content. It will be used as the teaser/preview portion of the post.
     *
     * @param htmlPostContent The HTML code of the post content
     * @return String - The first paragraph as HTML code
     */
    private String extractPostContentFirstParagraph(String htmlPostContent) {
        Document doc = Jsoup.parse(htmlPostContent);
        Element paragraph = doc
                .getElementsByClass("post-content").first()
                .getElementsByTag("p").first();
        return paragraph.outerHtml();
    }

    private boolean isHtmlFile(String filePath) {
        return filePath.endsWith(".htm") || filePath.endsWith(".html");
    }

    private List<File> extractAllHtmlFilesFromDirectory() {
        File[] htmlFileFolders = new File(LOCAL_HTML_POSTS_PATH).listFiles(File::isDirectory);
        System.out.println(htmlFileFolders.length);
        List<File> htmlFiles = new ArrayList<>();
        for (File folder : htmlFileFolders) {
            for (File htmlFile : folder.listFiles(File::isFile)) {
                htmlFiles.add(htmlFile);
            }
        }
        return htmlFiles;
    }

    /**
     * retrieves date of the first commit of the provided file
     *
     * @param repoFilePath - provided file
     * @return Date
     */
    private Date retrieveCommitTime(String repoFilePath, CommitOrder order) {
        Date commitTime = null;
        Map<String, List<Date>> commitTimes = repoService.retrieveCommitTimesOfPostFiles();
        Set<String> filePaths = commitTimes.keySet();
        if (filePaths.contains(repoFilePath)) {
            switch (order) {
                case FIRST:
                    commitTime = commitTimes.get(repoFilePath).get(0);
                    break;
                case LAST:
                    int last = commitTimes.get(repoFilePath).size() - 1;
                    commitTime = commitTimes.get(repoFilePath).get(last);
                    break;
                default:
                    commitTime = commitTimes.get(repoFilePath).get(0);
            }
        }
        return commitTime;
    }

    /**
     * Configures which commit should be retrieved with the method retrieveCommitTime(String, CommitOrder).
     */
    enum CommitOrder {
        // First commit of file (for created date)
        FIRST,
        // Last commit of file (for modified date)
        LAST
    }
}