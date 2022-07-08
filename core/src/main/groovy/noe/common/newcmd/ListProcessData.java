package noe.common.newcmd;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * DTO object which consists list of parsed results from system native command
 * listing processes.
 * This class then offers sorting capabilities over the parsed results.
 */
public class ListProcessData {
    /**
     * This is a map which represents lines of the output of ps command. Each line is then
     * represented by another map where each record is mapped to a column from the ps output.
     * This means that e.g. line '1 cat ochaloup' will be transfered to:
     * Map<1, Map<PID:1, COMM: cat, USER: ochaloup>>
     */
    private Map<String,Map<PsCmdFormat,String>> listing = new HashMap<String, Map<PsCmdFormat,String>>();


    /**
     * Appending values in process data. The underlying storage uses process id as key - the 'mapValue'
     * HAS TO contain {@link PsCmdFormat#PROCESS_ID} otherwise {@link IllegalArgumentException} will be
     * thrown.
     *
     * @param mapValue  map of values with {@link PsCmdFormat} as keys and string as parsed value
     * @return this
     * @throws IllegalArgumentException  when param does not contain data for {@link PsCmdFormat#PROCESS_ID}
     * @throws NullPointerException  when argument is null
     */
    public ListProcessData add(final Map<PsCmdFormat,String> mapValue) {
        Preconditions.checkNotNull(mapValue, "Data to add can't be null");
        if(mapValue.get(PsCmdFormat.PROCESS_ID) == null) {
            throw new IllegalArgumentException("mapValue parameter has to contain dat for PsCmdFormat#PROCESS_ID");
        }
        return this.add(mapValue.get(PsCmdFormat.PROCESS_ID), mapValue);
    }

    /**
     * Appending data of particular process id {@link PsCmdFormat#PROCESS_ID} just in case that
     * internal storage already contains data with such id. Otherwise will  be done nothing.
     * The underlying storage uses process id as key - the 'mapValue'HAS TO contain {@link PsCmdFormat#PROCESS_ID}
     * otherwise {@link IllegalArgumentException} will be
     * thrown.
     *
     * @param mapValue  value to be added
     * @return  this
     */
    public ListProcessData addIfExists(final Map<PsCmdFormat,String> mapValue) {
        Preconditions.checkNotNull(mapValue, "Data to add can't be null");
        if(mapValue.get(PsCmdFormat.PROCESS_ID) == null) {
            throw new IllegalArgumentException("mapValue parameter has to contain dat for PsCmdFormat#PROCESS_ID");
        }
        Map<PsCmdFormat,String> listOfValues = listing.get(mapValue.get(PsCmdFormat.PROCESS_ID));
        if(listOfValues != null) {
            this.add(mapValue.get(PsCmdFormat.PROCESS_ID), mapValue);
        }
        return this;
    }

    /**
     * Putting the map values to data storage and replacing data for the same PID if they exists in the storage.
     * If not just adding them.
     * Parameter 'mapValue' HAS TO contain {@link PsCmdFormat#PROCESS_ID} otherwise {@link IllegalArgumentException} will be
     * thrown.
     *
     * @param mapValue  data to be added to storage
     * @return this
     * @throws IllegalArgumentException  when param does not contain data for {@link PsCmdFormat#PROCESS_ID}
     * @throws NullPointerException  when argument is null
     */
    public ListProcessData put(final Map<PsCmdFormat,String> mapValue) {
        Preconditions.checkNotNull(mapValue, "Data to put can't be null");
        if(mapValue.get(PsCmdFormat.PROCESS_ID) == null) {
            throw new IllegalArgumentException("mapValue parameter has to contain dat for PsCmdFormat#PROCESS_ID");
        }
        return this.put(mapValue.get(PsCmdFormat.PROCESS_ID), mapValue);
    }

    /**
     * See {@link #put(Map)} just adding all data from list.
     *
     * @param mapValues  list of map to add to this storage
     * @return this
     */
    public ListProcessData putAll(final Iterable<Map<PsCmdFormat,String>> mapValues) {
        for(Map<PsCmdFormat,String> map: mapValues) {
            this.put(map);
        }
        return this;
    }

    /**
     * Number of lines that were returned by ps command and are currently available from data storage.
     * This number could change afte filter is applied.
     *
     * @return  number of lines
     */
    public int size() {
        return listing.size();
    }

    /**
     * On behalf of process id it returns it's representation in data by Map of keys and values.
     * As process id is unique value there can't be any duplication and just one item is returned.
     *
     * @param processId  process id that should be search in data
     * @return info on data
     */
    public Map<PsCmdFormat,String> get(final long processId) {
        String processIdAsString = String.valueOf(processId);
        if(listing.get(processIdAsString) == null) {
            return null;
        }
        // creating copy of data
        return new HashMap<PsCmdFormat,String>(listing.get(processIdAsString));
    }

    /**
     * Checking if the data contains line with column of {@link PsCmdFormat}
     * and value of value
     *
     * @param psCmdFormat  what value to search
     * @param value  what value should be contained
     */
    public boolean contains(final PsCmdFormat psCmdFormat, final String value) {
        for(Map<PsCmdFormat, String> line: listing.values()) {
            if(line.get(psCmdFormat) != null && line.get(psCmdFormat).equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returning copy of data from the process listing as string.
     *
     * @return  list where record represents line and map represents columns for that line
     */
    public List<Map<PsCmdFormat, String>> getAsList() {
        return new ArrayList<Map<PsCmdFormat, String>>(listing.values());
    }

    /**
     * Returning copy of data from the process listing as map with process id used as key.
     *
     * @return  map where each record represents line indexed by process id and
     *          inner map represents columns of the line
     */
    public Map<String,Map<PsCmdFormat,String>> getAsMap() {
        return new HashMap<String,Map<PsCmdFormat,String>>(listing);
    }

    /**
     * Returns list of parsed data from native 'ps' (process listing) command sorted
     * by some of columns defined as argument.
     *
     * @param formatsToSort  what columns should be taken for sorting
     * @return list of map where each record of list means one line of ps command output
     *         and map represents columns from the ps output
     */
    public List<Map<PsCmdFormat, String>> sortByAsList(final PsCmdFormat... formatsToSort) {
        return sortBy(false, formatsToSort);
    }

    /**
     * Returns list of parsed data from native 'ps' (process listing) command sorted
     * by some of columns defined as argument in reverse order.
     *
     * @param formatsToSort  what columns to sort by
     * @return list of map where each record of list means one line of ps command output
     *         and map represents columns from the ps output in reverse order
     */
    public List<Map<PsCmdFormat, String>> sortReverseByAsList(final PsCmdFormat... formatsToSort) {
        return sortBy(true, formatsToSort);
    }

    /**
     * Filter out all lines of results that satisfy predicate where {@link PsCmdFormat} value
     * is equal to string defined in second argument
     *
     * @param cmdFormat  column where to search for the contains string
     * @param equalsString  what to search
     * @return  filtered copy of data
     */
    public ListProcessData filterBy(final PsCmdFormat cmdFormat, final String equalsString) {
        ListProcessDataPredicate predicate = new ListProcessDataPredicate(cmdFormat, equalsString, true);
        return filterBy(predicate);
    }

    /**
     * Filter out all lines of results that satisfy predicate where {@link PsCmdFormat} value
     * contains a string defined in second argument
     *
     * @param cmdFormat  column where to search for the contains string
     * @param containsString  what to search
     * @return  filtered copy of data
     */
    public ListProcessData filterByContains(final PsCmdFormat cmdFormat, final String containsString) {
        ListProcessDataPredicate predicate = new ListProcessDataPredicate(cmdFormat, containsString, false);
        return filterBy(predicate);
    }

    /**
     * See {@link #filterBy(PsCmdFormat, String)} but removes.
     *
     * @param cmdFormat  cmd format that will define data to remove line by
     * @param equalsString  what the value of cmdFormat should be
     * @return  copy of ListProcessData with removed items
     */
    public ListProcessData removeBy(final PsCmdFormat cmdFormat, final String equalsString) {
        ListProcessDataPredicate predicate = new ListProcessDataPredicate(cmdFormat, equalsString, true);
        return removeBy(predicate);
    }

    /**
     * See {@link #filterByContains(PsCmdFormat, String)} but removes.
     *
     * @param cmdFormat  cmd format that will define data to remove line by
     * @param containsString  what the value of cmdFormat should be
     * @return  copy of ListProcessData with removed items
     */
    public ListProcessData removeByContains(final PsCmdFormat cmdFormat, final String containsString) {
        ListProcessDataPredicate predicate = new ListProcessDataPredicate(cmdFormat, containsString, false);
        return removeBy(predicate);
    }

    /**
     * Working directly on internal data structure and deletes all records/lines that are not complete.
     * Complete means that it does not have all records that is expected to exist for the listing.
     * This incompleteness could occur because of sever calls of ps external command and each of the call
     * could return a bit different data.
     */
    void removeIncomplete(final int expectedNumberOfRecords) {
        Iterator<String> listingIterator = listing.keySet().iterator();
        while (listingIterator.hasNext()) {
           String key = listingIterator.next();
           // there could be added some necessary items to listing (e.g. pid) as addition to expected
           // records that user wants to work with
           if(listing.get(key).size() < expectedNumberOfRecords) {
               listingIterator.remove();
           }
        }
    }

    private ListProcessData filterBy(final ListProcessDataPredicate predicate) {
        Iterable<Map<PsCmdFormat, String>> filteredList = Iterables.filter(getAsList(), predicate);
        ListProcessData copyOfData = new ListProcessData();
        return copyOfData.putAll(filteredList);
    }

    private ListProcessData removeBy(final ListProcessDataPredicate predicate) {
        List<Map<PsCmdFormat, String>> asList = getAsList();
        Iterables.removeIf(asList, predicate);
        ListProcessData copyOfData = new ListProcessData();
        return copyOfData.putAll(asList);
    }

    private List<Map<PsCmdFormat, String>> sortBy(final boolean isReverse, final PsCmdFormat... formatsToSort) {
        if(formatsToSort == null) {
            return getAsList();
        }
        Ordering<Map<PsCmdFormat, String>> ordering = null;
        for(PsCmdFormat psCmdFormat: formatsToSort) {
            if(psCmdFormat == null) {
                continue;
            }
            if(ordering == null) {
                ordering = isReverse ? (new ListProcessDataOrdering(psCmdFormat)).reverse() : new ListProcessDataOrdering(psCmdFormat);
            } else {
                ordering = isReverse ? ordering.compound((new ListProcessDataOrdering(psCmdFormat)).reverse()) :
                    ordering.compound(new ListProcessDataOrdering(psCmdFormat));
            }
        }
        return ordering == null ? getAsList() : ordering.sortedCopy(listing.values());
    }

    static class ListProcessDataOrdering extends Ordering<Map<PsCmdFormat, String>> {
        private PsCmdFormat psCmdFormatToSort;
        /**
         * Definition of comparator when saying what is the map should be compared by.
         *
         * @param psCmdFormatToSort  is parameter which defines sorting key of the map
         *        the sorting key is defined as 'ps' command record type - see {@link PsCmdFormat}
         */
        public ListProcessDataOrdering(final PsCmdFormat psCmdFormatToSort) {
            this.psCmdFormatToSort = psCmdFormatToSort;
        }
        @Override
        public int compare(final Map<PsCmdFormat, String> left, final Map<PsCmdFormat, String> right) {
            if(left == null && right == null) {
                // both maps are null
                return 0;
            }
            if(left != null && left.get(psCmdFormatToSort) == null &&
               right != null && right.get(psCmdFormatToSort) == null) {
                // maps are not null but neither one contain the key that we want to sort by
                return 0;
            }
            if(left == null || left.get(psCmdFormatToSort) == null) {
                return -1;
            }
            if(right == null || right.get(psCmdFormatToSort) == null) {
                return 1;
            }

            String str1 = left.get(psCmdFormatToSort);
            String str2 = right.get(psCmdFormatToSort);
            Class<?> dataType = psCmdFormatToSort.getDataType();
            if(dataType.isInstance(Integer.valueOf(0))) {
                boolean int1error = false;
                Integer int1 = null, int2 = null;
                try {
                    int1 = Integer.valueOf(str1);
                } catch (NumberFormatException nfe) {
                    int1error = true;
                }
                try {
                    int2 = Integer.valueOf(str2);
                } catch (NumberFormatException nfe) {
                    // if string from left is not possible to convert to int
                    // then in case of right is not possible to convert do string comparison
                    // otherwise left was possible to convert but right is not possible to convert (returns 1)
                    return int1error ? str1.compareTo(str2) : 1;
                }
                // if string from left is not possible to convert to int
                // then we know that right was converted fine and we return -1
                // otherwise both were converted without problem and returning integer comparison
                return int1error ? -1 : int1.compareTo(int2);
            }
            if(dataType.isInstance(Long.valueOf(0))) {
                boolean long1error = false;
                Long long1 = null, long2 = null;
                try {
                    long1 = Long.valueOf(str1);
                } catch (NumberFormatException nfe) {
                    long1error = true;
                }
                try {
                    long2 = Long.valueOf(str2);
                } catch (NumberFormatException nfe) {
                    // if string from left is not possible to convert to long
                    // then in case of right is not possible to convert do string comparison
                    // otherwise left was possible to convert but right is not possible to convert (returns 1)
                    return long1error ? str1.compareTo(str2) : 1;
                }
                // if string from left is not possible to convert to long
                // then we know that right was converted fine and we return -1
                // otherwise both were converted without problem and returning integer comparison
                return long1error ? -1 : long1.compareTo(long2);
            }
            return str1.compareTo(str2);
        }
    }

    static class ListProcessDataPredicate implements Predicate<Map<PsCmdFormat, String>> {
        private PsCmdFormat psCmdFormatToVerify;
        private String stringToVerify;
        private boolean isEqual = true;
        /**
         * Equality set to true
         */
        public ListProcessDataPredicate(final PsCmdFormat psCmdFormatToVerify, final String stringToVerify) {
            this(psCmdFormatToVerify, stringToVerify, true);
        }
        public ListProcessDataPredicate(final PsCmdFormat psCmdFormatToVerify, final String stringToVerify, final boolean isEqual) {
            Preconditions.checkNotNull(psCmdFormatToVerify, "PsCmdFormat for predicate can't be null");
            Preconditions.checkNotNull(stringToVerify, "String to verify for predicate can't be null");
            this.psCmdFormatToVerify = psCmdFormatToVerify;
            this.stringToVerify = stringToVerify;
            this.isEqual = isEqual;
        }

        public boolean apply(final Map<PsCmdFormat, String> input) {
            if(input == null || input.get(psCmdFormatToVerify) == null) {
                return false;
            }
            if(isEqual) {
                return input.get(psCmdFormatToVerify).equals(stringToVerify);
            } else {
                return input.get(psCmdFormatToVerify).contains(stringToVerify);
            }
        }

    }

    private ListProcessData add(final String key, final Map<PsCmdFormat,String> mapValue) {
        Map<PsCmdFormat,String> listOfValues = listing.get(key);
        if(listOfValues == null) {
            listOfValues = new HashMap<PsCmdFormat,String>();
            listing.put(key, listOfValues);
        }
        listOfValues.putAll(mapValue);
        return this;
    }

    private ListProcessData put(final String key, final Map<PsCmdFormat, String> mapValue) {
        listing.put(key, new HashMap<PsCmdFormat, String>(mapValue));
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(String key: listing.keySet()) {
            sb.append("{[" + key + "]");
            for(Entry<PsCmdFormat,String> item: listing.get(key).entrySet()) {
                sb.append(item.getKey() + ":" + item.getValue() + ",");
            }
            sb.append("}");
        }
        return sb.toString();
    }
}
