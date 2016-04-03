package File_Query;

public class VocabularyInfo
{
    public double idf;
    public int bytes;
    public int buf;
    
    VocabularyInfo(double idf, int bytes, int buf)
    {
        this.idf = idf;
        this.bytes = bytes;
        this.buf = buf;
    }
}
