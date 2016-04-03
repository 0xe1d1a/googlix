package Rss_Information_Retrieval;

import java.util.SortedMap;

public class Test
{
    public static void main(String[] args) throws Exception
    {
        RssIndex  index = new RssIndex();
        SortedMap<String,Short> sortedmap;
        long startTime = System.currentTimeMillis();
        index.readStopWords("stopWords.txt");
        sortedmap=index.FileMainOperations("RssFeeds");   
        index.GenerateFiles(sortedmap);
        long stopTime = System.currentTimeMillis();
        System.out.println(stopTime - startTime);
    }
}
