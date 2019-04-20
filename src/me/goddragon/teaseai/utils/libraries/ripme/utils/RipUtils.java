package me.goddragon.teaseai.utils.libraries.ripme.utils;

import me.goddragon.teaseai.utils.TeaseLogger;
import me.goddragon.teaseai.utils.libraries.ripme.ripper.AbstractRipper;
import me.goddragon.teaseai.utils.libraries.ripme.ripper.rippers.*;
import org.apache.commons.lang.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RipUtils {
    private static final TeaseLogger logger = TeaseLogger.getLogger();

    public static List<URL> getFilesFromURL(URL url) {
        List<URL> result = new ArrayList<>();

        logger.log(Level.FINE, "Checking " + url);
        // Imgur album
        if ((url.getHost().endsWith("imgur.com"))
                && url.toExternalForm().contains("imgur.com/a/")) {
            try {
                logger.log(Level.FINE, "Fetching imgur album at " + url);
                ImgurRipper.ImgurAlbum imgurAlbum = ImgurRipper.getImgurAlbum(url);
                for (ImgurRipper.ImgurImage imgurImage : imgurAlbum.images) {
                    logger.log(Level.FINE, "Got imgur image: " + imgurImage.url);
                    result.add(imgurImage.url);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "[!] Exception while loading album " + url, e);
            }
            return result;
        }
        else if (url.getHost().endsWith("imgur.com") && url.toExternalForm().contains(",")) {
            // Imgur image series.
            try {
                logger.log(Level.FINE, "Fetching imgur series at " + url);
                ImgurRipper.ImgurAlbum imgurAlbum = ImgurRipper.getImgurSeries(url);
                for (ImgurRipper.ImgurImage imgurImage : imgurAlbum.images) {
                    logger.log(Level.FINE, "Got imgur image: " + imgurImage.url);
                    result.add(imgurImage.url);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "[!] Exception while loading album " + url, e);
            }
        }  else if (url.getHost().endsWith("i.imgur.com") && url.toExternalForm().contains("gifv")) {
            // links to imgur gifvs
            try {
                result.add(new URL(url.toExternalForm().replaceAll(".gifv", ".mp4")));
            } catch (IOException e) {
                logger.log(Level.INFO, "Couldn't get gifv from " + url);
            }
            return result;

        }
        else if (url.getHost().endsWith("gfycat.com")) {
            try {
                logger.log(Level.FINE, "Fetching gfycat page " + url);
                String videoURL = GfycatRipper.getVideoURL(url);
                logger.log(Level.FINE, "Got gfycat URL: " + videoURL);
                result.add(new URL(videoURL));
            } catch (IOException e) {
                // Do nothing
                logger.log(Level.WARNING, "Exception while retrieving gfycat page:", e);
            }
            return result;
        }
        else if (url.toExternalForm().contains("vidble.com/album/") || url.toExternalForm().contains("vidble.com/show/")) {
            try {
                logger.log(Level.INFO, "Getting vidble album " + url);
                result.addAll(VidbleRipper.getURLsFromPage(url));
            } catch (IOException e) {
                // Do nothing
                logger.log(Level.WARNING, "Exception while retrieving vidble page:", e);
            }
            return result;
        }
        else if (url.toExternalForm().contains("eroshare.com")) {
            try {
                logger.log(Level.INFO, "Getting eroshare album " + url);
                result.addAll(EroShareRipper.getURLs(url));
            } catch (IOException e) {
                // Do nothing
                logger.log(Level.WARNING, "Exception while retrieving eroshare page:", e);
            }
            return result;
        } else if (url.toExternalForm().contains("v.redd.it")) {
            result.add(url);
            return result;
        }

        else if (url.toExternalForm().contains("erome.com")) {
            try {
                logger.log(Level.INFO, "Getting eroshare album " + url);
                EromeRipper r = new EromeRipper(url);
                Document tempDoc = r.getFirstPage();
                for (String u : r.getURLsFromPage(tempDoc)) {
                    result.add(new URL(u));
                }
            } catch (IOException e) {
                // Do nothing
                logger.log(Level.WARNING, "Exception while retrieving eroshare page:", e);
            }
            return result;
        }

        Pattern p = Pattern.compile("https?://i.reddituploads.com/([a-zA-Z0-9]+)\\?.*");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            logger.log(Level.INFO, "URL: " + url.toExternalForm());
            String u = url.toExternalForm().replaceAll("&amp;", "&");
            try {
                result.add(new URL(u));
            } catch (MalformedURLException e) {
            }
            return result;
        }

        // Direct link to image
        p = Pattern.compile("(https?://[a-zA-Z0-9\\-.]+\\.[a-zA-Z]{2,3}(/\\S*)\\.(jpg|jpeg|gif|png|mp4)(\\?.*)?)");
        m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            try {
                URL singleURL = new URL(m.group(1));
                logger.log(Level.FINE, "Found single URL: " + singleURL);
                result.add(singleURL);
                return result;
            } catch (MalformedURLException e) {
                logger.log(Level.SEVERE, "[!] Not a valid URL: '" + url + "'", e);
            }
        }

        if (url.getHost().equals("imgur.com") ||
                url.getHost().equals("m.imgur.com")) {
            try {
                // Fetch the page
                Document doc = Jsoup.connect(url.toExternalForm())
                        .userAgent(AbstractRipper.USER_AGENT)
                        .get();
                for (Element el : doc.select("meta")) {
                    if (el.attr("property").equals("og:video")) {
                        result.add(new URL(el.attr("content")));
                        return result;
                    }
                    else if (el.attr("name").equals("twitter:image:src")) {
                        result.add(new URL(el.attr("content")));
                        return result;
                    }
                    else if (el.attr("name").equals("twitter:image")) {
                        result.add(new URL(el.attr("content")));
                        return result;
                    }
                }
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "[!] Error", ex);
            }

        }

        logger.log(Level.SEVERE, "[!] Unable to rip URL: " + url);
        return result;
    }

    public static Pattern getURLRegex() {
        return Pattern.compile("(https?://[a-zA-Z0-9\\-.]+\\.[a-zA-Z]{2,3}(/\\S*))");
    }

    public static String urlFromDirectoryName(String dir) {
        String url = null;
        if (url == null) url = urlFromImgurDirectoryName(dir);
        if (url == null) url = urlFromImagefapDirectoryName(dir);
        if (url == null) url = urlFromDeviantartDirectoryName(dir);
        if (url == null) url = urlFromRedditDirectoryName(dir);
        if (url == null) url = urlFromSiteDirectoryName(dir, "bfcakes",     "http://www.bcfakes.com/celebritylist/", "");
        if (url == null) url = urlFromSiteDirectoryName(dir, "drawcrowd",   "http://drawcrowd.com/", "");
        if (url == null) url = urlFromSiteDirectoryName(dir.replace("-", "/"), "ehentai", "http://g.e-hentai.org/g/", "");
        if (url == null) url = urlFromSiteDirectoryName(dir, "vinebox", "http://finebox.co/u/", "");
        if (url == null) url = urlFromSiteDirectoryName(dir, "imgbox", "http://imgbox.com/g/", "");
        if (url == null) url = urlFromSiteDirectoryName(dir, "modelmayhem", "http://www.modelmayhem.com/", "");
        //if (url == null) url = urlFromSiteDirectoryName(dir, "8muses",      "http://www.8muses.com/index/category/", "");
        return url;
    }

    private static String urlFromSiteDirectoryName(String dir, String site, String before, String after) {
        if (!dir.startsWith(site + "_")) {
            return null;
        }
        dir = dir.substring((site + "_").length());
        return before + dir + after;
    }

    private static String urlFromRedditDirectoryName(String dir) {
        if (!dir.startsWith("reddit_")) {
            return null;
        }
        String url = null;
        String[] fields = dir.split("_");
        switch (fields[0]) {
            case "sub":
                url = "http://reddit.com/r/" + dir;
                break;
            case "user":
                url = "http://reddit.com/user/" + dir;
                break;
            case "post":
                url = "http://reddit.com/comments/" + dir;
                break;
        }
        return url;
    }

    private static String urlFromImagefapDirectoryName(String dir) {
        if (!dir.startsWith("imagefap")) {
            return null;
        }
        String url = null;
        dir = dir.substring("imagefap_".length());
        if (NumberUtils.isDigits(dir)) {
            url = "http://www.imagefap.com/gallery.php?gid=" + dir;
        }
        else {
            url = "http://www.imagefap.com/gallery.php?pgid=" + dir;
        }
        return url;
    }

    private static String urlFromDeviantartDirectoryName(String dir) {
        if (!dir.startsWith("deviantart")) {
            return null;
        }
        dir = dir.substring("deviantart_".length());
        String url = null;
        if (!dir.contains("_")) {
            url = "http://" + dir + ".deviantart.com/";
        }
        else {
            String[] fields = dir.split("_");
            url = "http://" + fields[0] + ".deviantart.com/gallery/" + fields[1];
        }
        return url;
    }

    private static String urlFromImgurDirectoryName(String dir) {
        if (!dir.startsWith("imgur_")) {
            return null;
        }
        if (dir.contains(" ")) {
            dir = dir.substring(0, dir.indexOf(" "));
        }
        List<String> fields = Arrays.asList(dir.split("_"));
        String album = fields.get(1);
        String url = "http://";
        if ((fields.contains("top") || fields.contains("new"))
                && (fields.contains("year") || fields.contains("month") || fields.contains("week") || fields.contains("all"))) {
            // Subreddit
            fields.remove(0); // "imgur"
            String sub = "";
            while (fields.size() > 2) {
                if (!sub.equals("")) {
                    sub += "_";
                }
                sub = fields.remove(0); // Subreddit that may contain "_"
            }
            url += "imgur.com/r/" + sub + "/";
            url += fields.remove(0) + "/";
            url += fields.remove(0);
        }
        else if (album.contains("-")) {
            // Series of images
            url += "imgur.com/" + album.replaceAll("-", ",");
        }
        else if (album.length() == 5 || album.length() == 6) {
            // Album
            url += "imgur.com/a/" + album;
        }
        else {
            // User account
            url += album + ".imgur.com/";
            if (fields.size() > 2) {
                url += fields.get(2);
            }
        }
        return url;
    }
    /**
     * Reads a cookie string (Key1=value1;key2=value2) from the config file and turns it into a hashmap
     * @return Map of cookies containing session data.
     */
    public static Map<String, String> getCookiesFromString(String line) {
        Map<String,String> cookies = new HashMap<>();
        for (String pair : line.split(";")) {
            String[] kv = pair.split("=");
            cookies.put(kv[0], kv[1]);
        }
        return cookies;
    }

    /**
     * Checks for blacklisted tags on page. If it finds one it returns it, if not it return null
     *
     * @param blackListedTags a string array of the blacklisted tags
     * @param tagsOnPage the tags on the page
     * @return String
     */
    public static String checkTags(String[] blackListedTags, List<String> tagsOnPage) {
        // If the user hasn't blacklisted any tags we return null;
        if (blackListedTags == null) {
            return null;
        }
        for (String tag : blackListedTags) {
            for (String pageTag : tagsOnPage) {
                // We replace all dashes in the tag with spaces because the tags we get from the site are separated using
                // dashes
                if (tag.trim().toLowerCase().equals(pageTag.toLowerCase())) {
                    return tag.toLowerCase();
                }
            }
        }
        return null;
    }
}
