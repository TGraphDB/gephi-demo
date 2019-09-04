package me;

import org.act.tgraph.demo.utils.Helper;
import org.act.tgraph.demo.utils.TransactionWrapper;
import org.junit.Test;

/**
 * Created by song on 16-5-27.
 */
public class HelperTimeConvertTest
{
    @Test
    public void testTime()
    {
        System.out.println( Helper.timeStamp2String( Helper.timeStr2int( "201011040000" ) ) );

    }

}
