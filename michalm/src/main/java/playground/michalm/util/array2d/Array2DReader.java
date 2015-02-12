package playground.michalm.util.array2d;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;


/**
 * Reads 2D arrays (of different types) from files. To handle different types of data use
 * <code>TypeStrategy</code>. There are some predefined strategies for following types: double, int,
 * String.
 * 
 * @author michalm
 */
public class Array2DReader
{
    public static final Strategy DOUBLE_STRATEGY = new DoubleStrategy();
    public static final Strategy INT_STRATEGY = new IntStrategy();
    public static final Strategy STRING_STRATEGY = new StringStrategy();


    public static double[][] getDoubleArray(File file, int cols)
    {
        return (double[][])getArray(file, cols, DOUBLE_STRATEGY);
    }


    public static int[][] getIntArray(File file, int cols)
    {
        return (int[][])getArray(file, cols, INT_STRATEGY);
    }


    public static String[][] getStringArray(File file, int cols)
    {
        return (String[][])getArray(file, cols, STRING_STRATEGY);
    }


    public static Object getArray(File file, int cols, Strategy strategy)
    {
        try {
            return getArray(new FileReader(file), cols, strategy);
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException();
        }
    }


    public static Object getArray(Reader reader, int cols, Strategy strategy)
    {
        List<Object> rows = new ArrayList<>();
        boolean endOfArray = false;

        try (@SuppressWarnings("resource")
        BufferedReader br = new BufferedReader(reader)) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                StringTokenizer st = new StringTokenizer(line, " \t");

                if (endOfArray) {
                    if (st.hasMoreTokens()) {
                        throw new RuntimeException("Non-empty line after matrix");
                    }
                }
                else {
                    if (!st.hasMoreTokens()) {
                        endOfArray = true;
                        continue;
                    }

                    Object row = strategy.createRow(cols);

                    for (int i = 0; i < cols; i++) {
                        if (!st.hasMoreTokens()) {
                            throw new RuntimeException("Too few elements");
                        }

                        strategy.addToRow(row, i, st.nextToken());
                    }

                    if (st.hasMoreTokens()) {
                        throw new RuntimeException("Too many elements");
                    }

                    rows.add(row);
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (rows.size() == 0) {
            return null;
        }

        Object row0 = rows.get(0);
        Class<?> clazz = row0 != null ? row0.getClass() : Object.class;

        return rows.toArray((Object[])Array.newInstance(clazz, rows.size()));
    }


    public static interface Strategy
    {
        Object createRow(int cols);


        void addToRow(Object row, int col, String element);
    }


    public static class DoubleStrategy
        implements Strategy
    {
        public Object createRow(int cols)
        {
            return new double[cols];
        }


        public void addToRow(Object row, int idx, String element)
        {
            ((double[])row)[idx] = Double.parseDouble(element);
        }
    }


    public static class IntStrategy
        implements Strategy
    {
        public Object createRow(int cols)
        {
            return new int[cols];
        }


        public void addToRow(Object row, int idx, String element)
        {
            ((int[])row)[idx] = Integer.parseInt(element);
        }
    }


    public static class StringStrategy
        implements Strategy
    {
        public Object createRow(int cols)
        {
            return new String[cols];
        }


        public void addToRow(Object row, int idx, String element)
        {
            ((String[])row)[idx] = element;
        }
    }
}
