package noe.common.newcmd;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static noe.common.newcmd.PsCmdFormat.COMMAND;
import static noe.common.newcmd.PsCmdFormat.PARENT_PROCESS_ID;
import static noe.common.newcmd.PsCmdFormat.PROCESS_ID;
import static noe.common.newcmd.PsCmdFormat.USER;

public class ListProcessDataTest {
    ListProcessData data;

    @Before
    public void setup() {
        data = new ListProcessData();
        Map<PsCmdFormat, String> record = new HashMap<PsCmdFormat, String>();
        record.put(PROCESS_ID, "0");
        record.put(COMMAND, "havearing");
        record.put(USER, "bilbo");
        data.add(record);
        record.put(PROCESS_ID, "100");
        record.put(COMMAND, "bedrunk");
        record.put(USER, "lister");
        record.put(PARENT_PROCESS_ID, "0");
        data.add(record);
        record.put(PROCESS_ID, "42");
        record.put(COMMAND, "hitchhike");
        record.put(USER, "arthur");
        data.add(record);
        record.put(PROCESS_ID, "24");
        record.put(COMMAND, "haveatowel");
        record.put(USER, "arthur");
        record.put(PARENT_PROCESS_ID, "42");
        data.add(record);
        record.put(PROCESS_ID, "443");
        record.put(COMMAND, "fridge");
        record.put(USER, "arthur");
        data.add(record);
    }

    @Test
    public void testBasicSorting() {
        List<Map<PsCmdFormat, String>> sortedData = data.sortByAsList(PROCESS_ID);
        Assert.assertEquals("Four items of data should be get after sorting", 5, sortedData.size());
        Assert.assertEquals("0", sortedData.get(0).get(PROCESS_ID));
        Assert.assertEquals("24", sortedData.get(1).get(PROCESS_ID));
        Assert.assertEquals("42", sortedData.get(2).get(PROCESS_ID));
        Assert.assertEquals("100", sortedData.get(3).get(PROCESS_ID));
        Assert.assertEquals("443", sortedData.get(4).get(PROCESS_ID));

        sortedData = data.sortReverseByAsList(PROCESS_ID);
        Assert.assertEquals("Four items of data should be get after sorting", 5, sortedData.size());
        Assert.assertEquals("443", sortedData.get(0).get(PROCESS_ID));
        Assert.assertEquals("100", sortedData.get(1).get(PROCESS_ID));
        Assert.assertEquals("42", sortedData.get(2).get(PROCESS_ID));
        Assert.assertEquals("24", sortedData.get(3).get(PROCESS_ID));
        Assert.assertEquals("0", sortedData.get(4).get(PROCESS_ID));

        sortedData = data.sortReverseByAsList(null, PROCESS_ID, null);
        Assert.assertEquals("Four items of data should be get after sorting", 5, sortedData.size());
        Assert.assertEquals("443", sortedData.get(0).get(PROCESS_ID));
        Assert.assertEquals("100", sortedData.get(1).get(PROCESS_ID));
        Assert.assertEquals("42", sortedData.get(2).get(PROCESS_ID));
        Assert.assertEquals("24", sortedData.get(3).get(PROCESS_ID));
        Assert.assertEquals("0", sortedData.get(4).get(PROCESS_ID));

        PsCmdFormat iAmNull = null;
        sortedData = data.sortByAsList(iAmNull);
        Assert.assertEquals("Four items of data should be get after sorting", 5, sortedData.size());
    }

    @Test
    public void testMultipleSorting() {
        List<Map<PsCmdFormat, String>> sortedData = data.sortByAsList(USER, PARENT_PROCESS_ID, PROCESS_ID);

        Assert.assertEquals("Four items of data should be get after sorting", 5, sortedData.size());
        Assert.assertEquals("42", sortedData.get(0).get(PROCESS_ID));
        Assert.assertEquals("24", sortedData.get(1).get(PROCESS_ID));
        Assert.assertEquals("443", sortedData.get(2).get(PROCESS_ID));
        Assert.assertEquals("0", sortedData.get(3).get(PROCESS_ID));
        Assert.assertEquals("100", sortedData.get(4).get(PROCESS_ID));
    }

    @Test
    public void testFilter() {
        ListProcessData filteredData = data.filterBy(USER, "arthur");
        Assert.assertEquals("Three items should be contained after filtering but it's" + filteredData.size(), 3, filteredData.size());
        Assert.assertTrue("Arthur should be contained", filteredData.contains(USER, "arthur"));
        Assert.assertFalse("Hobit should be filtered out", filteredData.contains(USER, "bilbo"));

        filteredData = data.filterByContains(PROCESS_ID, "0");
        Assert.assertEquals("Three items should be contained after filtering but it's" + filteredData.size(), 2, filteredData.size());
        Assert.assertFalse("Arthur should be filtered out", filteredData.contains(USER, "arthur"));
        Assert.assertTrue("Hobit should be taken by filter", filteredData.contains(USER, "bilbo"));
    }

    @Test
    public void testRemove() {
        ListProcessData filteredData = data.removeBy(USER, "arthur");
        Assert.assertEquals("Two items should be contained after filtering but it's" + filteredData.size(), 2, filteredData.size());
        Assert.assertFalse("Arthur should not be contained", filteredData.contains(USER, "arthur"));
        Assert.assertTrue("Hobit should not be removed", filteredData.contains(USER, "bilbo"));

        filteredData = data.removeByContains(PROCESS_ID, "0");
        Assert.assertEquals("Three items should be contained after filtering but it's" + filteredData.size(), 3, filteredData.size());
        Assert.assertTrue("Arthur should not be removed", filteredData.contains(USER, "arthur"));
        Assert.assertFalse("Hobit should be removed", filteredData.contains(USER, "bilbo"));
    }
}
