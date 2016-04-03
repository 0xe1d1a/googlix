
package File_Query;

import java.util.Comparator;

public class MyComparator implements Comparator<ScoreSet>
{
    @Override
    public int compare(ScoreSet o1, ScoreSet o2) {
        if (o1.score < o2.score)
        {
            return 1;
        }
        else if (o1.score > o2.score)
        {
            return -1;
        }
        else
        {
            return 0;
        }     
    }   
}
