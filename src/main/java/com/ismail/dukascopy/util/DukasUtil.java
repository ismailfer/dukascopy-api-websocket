package com.ismail.dukascopy.util;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.api.UpgradeRequest;

import com.ismail.dukascopy.model.ApiException;

/**
 * utility
 * 
 * @author ismail
 * @since 20220603
 */
public class DukasUtil {
    public static final long SECOND = 1000L;

    public static final long MINUTE = 60 * SECOND;

    public static final long HOUR = 3600 * SECOND;

    public static final long DAY = 24L * HOUR;

    public static final long WEEK = 7L * DAY;

    public static final long MONTH = 30L * DAY;

    public static final long YEAR = 365L * DAY;

    /**
     * Convert a string into HTML format string; by maintaining new lines etc
     * 
     * @param pInputString input string
     * @return html format
     */
    public static String toHtmlFormat(String pInputString) {
        if (pInputString == null || pInputString.length() == 0)
            return "";

        String str = pInputString.replaceAll("ï¿½", "'");

        str = str.replaceAll("<", "&lt;");
        str = str.replaceAll(">", "&gt;");

        // replace double spaces
        str = str.replaceAll("    ", "&nbsp;&nbsp;&nbsp;&nbsp;");
        str = str.replaceAll("   ", "&nbsp;&nbsp;&nbsp;");
        str = str.replaceAll("  ", "&nbsp;&nbsp;");

        // new line chars
        str = str.replaceAll("\n", "\n<br>\n");

        // Enable HTML links

        return str;
    }

    /**
     * @param pVal
     * @return
     */
    public final static boolean isDefined(final String pVal) {
        return (pVal != null && pVal.length() > 0);
    }

    // --------------------------------------------------------------------------------------------------
    // Web / Servlet utilities
    // --------------------------------------------------------------------------------------------------

    public static String getParameter(HttpServletRequest request, String pParamName) {
        String str = request.getParameter(pParamName);

        if (str != null)
            return str;
        else
            throw new ApiException("Required parameter missing: " + pParamName);
    }

    public static String getParameter(HttpServletRequest request, String pParamName, String pDefaultValue) {
        String str = request.getParameter(pParamName);

        if (str != null)
            return str;
        else
            return pDefaultValue;
    }

    public static char getParameterAsChar(HttpServletRequest request, String pParamName) {
        String str = request.getParameter(pParamName);

        if (str != null && str.length() > 0)
            return str.charAt(0);
        else
            throw new ApiException("Required parameter missing: " + pParamName);
    }

    public static char getParameterAsChar(HttpServletRequest request, String pParamName, char pDefaultValue) {
        String str = request.getParameter(pParamName);

        if (str != null && str.length() > 0)
            return str.charAt(0);
        else
            return pDefaultValue;
    }

    public static double getParameterAsDouble(HttpServletRequest request, String pParamName) {
        String str = request.getParameter(pParamName);

        if (str != null && str.length() > 0)
            return Double.parseDouble(str);
        else
            throw new ApiException("Required parameter missing: " + pParamName);
    }

    public static double getParameterAsDouble(HttpServletRequest request, String pParamName, double pDefaultValue) {
        String str = request.getParameter(pParamName);

        if (str != null && str.length() > 0)
            return Double.parseDouble(str);
        else
            return pDefaultValue;
    }

    public static int getParameterAsInt(HttpServletRequest request, String pParamName) {
        String str = request.getParameter(pParamName);

        if (str != null && str.length() > 0)
            return Integer.parseInt(str);
        else
            throw new ApiException("Required parameter missing: " + pParamName);
    }

    public static int getParameterAsInt(HttpServletRequest request, String pParamName, int pDefaultValue) {
        String str = request.getParameter(pParamName);

        if (str != null && str.length() > 0)
            return Integer.parseInt(str);
        else
            return pDefaultValue;
    }

    public static long getParameterAsLong(HttpServletRequest request, String pParamName) {
        String str = request.getParameter(pParamName);

        if (str != null && str.length() > 0)
            return Long.parseLong(str);
        else
            throw new ApiException("Required parameter missing: " + pParamName);
    }

    public static long getParameterAsLong(HttpServletRequest request, String pParamName, long pDefaultValue) {
        String str = request.getParameter(pParamName);

        if (str != null && str.length() > 0)
            return Long.parseLong(str);
        else
            return pDefaultValue;
    }

    public static boolean getParameterAsBoolean(HttpServletRequest request, String pParamName) {
        String str = request.getParameter(pParamName);

        if (str != null && str.length() > 0)
            return Boolean.parseBoolean(str);
        else
            throw new ApiException("Required parameter missing: " + pParamName);
    }

    public static boolean getParameterAsBoolean(HttpServletRequest request, String pParamName, boolean pDefaultValue) {
        String str = request.getParameter(pParamName);

        if (str != null && str.length() > 0)
            return Boolean.parseBoolean(str);
        else
            return pDefaultValue;
    }

    // --------------------------------------------------------------------------------------------------
    // Formatting utilities
    // --------------------------------------------------------------------------------------------------

    public static String get(UpgradeRequest request, String pParamName, String pDefaultValue) {
        String val = pDefaultValue;

        Map<String, List<String>> paramMap = request.getParameterMap();

        if (paramMap != null) {
            List<String> valList = paramMap.get(pParamName);

            if (valList != null) {
                val = valList.get(0);
            }
        }

        return val;
    }

    public static String get(UpgradeRequest request, String pParamName) {
        String val = get(request, pParamName, null);

        if (val != null)
            return val;
        else
            throw new IllegalArgumentException("Mandatory parameter is missing: " + pParamName);
    }

    public static boolean getb(UpgradeRequest request, String pParamName) {
        String str = get(request, pParamName);

        boolean val = false;

        // Since we are using checkboxes for booleans; when the box is unchecked; it is
        // not passed; so null really means false
        if (DukasUtil.isDefined(str))
            val = "T".equalsIgnoreCase(str) || "true".equalsIgnoreCase(str) || "y".equalsIgnoreCase(str);
        else
            throw new IllegalArgumentException("Parameter '" + pParamName + "' is required!");

        return val;
    }

    /**
     * @param request
     * @param pParamName
     * @param pDefaultValue
     * @return
     */
    public static boolean getb(UpgradeRequest request, String pParamName, boolean pDefaultValue) {
        String str = get(request, pParamName, null);

        boolean val = pDefaultValue;

        // Since we are using checkboxes for booleans; when the box is unchecked; it is
        // not passed; so null really means false
        if (DukasUtil.isDefined(str))
            val = "T".equalsIgnoreCase(str) || "true".equalsIgnoreCase(str) || "y".equalsIgnoreCase(str);

        return val;
    }

    public static String getRemoteAddress(SocketAddress socketAddr) {
        String addr = null;

        if (socketAddr instanceof InetSocketAddress) {
            InetSocketAddress isocket = (InetSocketAddress) socketAddr;
            addr = isocket.getAddress().getHostAddress();
        } else {
            addr = socketAddr.toString();
            if (addr.startsWith("/"))
                addr = addr.substring(1);

            int posAddr = addr.indexOf(':');
            if (posAddr != -1)
                addr = addr.substring(0, posAddr);
        }

        return addr;
    }

    // --------------------------------------------------------------------------------------------------
    // Formatting utilities
    // --------------------------------------------------------------------------------------------------

    public static final String numericFormat(double value, int decimals) {
        NumberFormat formatter = null;

        if (decimals <= 0) {
            formatter = new DecimalFormat("#0");

        } else {
            StringBuilder sb = new StringBuilder("#0.");
            for (int i = 0; i < decimals; i++)
                sb.append('0');

            formatter = new DecimalFormat(sb.toString());
        }

        return formatter.format(value);
    }

    public static SimpleDateFormat getFormatter(String pFormat) {
        return getFormatter(pFormat, null, false);
    }

    public static SimpleDateFormat getFormatter(String pFormat, String pTimeZoneID) {
        return getFormatter(pFormat, pTimeZoneID, false);
    }

    public static SimpleDateFormat getFormatter(String pFormat, String pTimeZoneID, boolean pLenient) {
        SimpleDateFormat dateParser = new SimpleDateFormat(pFormat);
        dateParser.setLenient(pLenient);

        if (pTimeZoneID != null) {
            TimeZone tz = TimeZone.getTimeZone(pTimeZoneID);
            dateParser.setTimeZone(tz);
        }

        return dateParser;
    }

    public static String getOrderAge(long pMillis) {
        String age = null;

        if (pMillis > DAY)
            age = getOrderAgeInHours(pMillis);
        else if (pMillis > HOUR)
            age = getOrderAgeInMinutes(pMillis);
        else if (pMillis > MINUTE)
            age = getOrderAgeInSeconds(pMillis);
        else
            age = getOrderAgeInMillis(pMillis);

        return age;
    }

    /**
     * Returns the time elapsed; but round it by days (i.e. don't show hours,
     * minutes, seconds..etc)
     * 
     * @param pMillis
     * @return
     */
    public static String getOrderAgeInSeconds(long pMillis) {
        if (pMillis <= 0L)
            return "";

        long millis = pMillis - pMillis % SECOND;

        return getOrderAgeInMillis(millis);
    }

    /**
     * Returns the time elapsed; but round it by days (i.e. don't show hours,
     * minutes, seconds..etc)
     * 
     * @param pMillis
     * @return
     */
    public static String getOrderAgeInMinutes(long pMillis) {
        if (pMillis <= 0L)
            return "";

        long millis = pMillis - pMillis % MINUTE;

        return getOrderAgeInMillis(millis);
    }

    /**
     * Returns the time elapsed; but round it by days (i.e. don't show hours,
     * minutes, seconds..etc)
     * 
     * @param pMillis
     * @return
     */
    public static String getOrderAgeInHours(long pMillis) {
        if (pMillis <= 0L)
            return "";

        long millis = pMillis - pMillis % HOUR;

        return getOrderAgeInMillis(millis);
    }

    /**
     * convert millis to a amount in years / months / days / hours / minutes /
     * seconds
     * 
     * and displays the time in a shorter version; depending on the duration
     * 
     * @param pMillis
     * @return
     */
    public static String getOrderAgeInMillis(long pMillis) {
        int sign = pMillis >= 0 ? 1 : -1;

        long millis = pMillis;
        if (sign < 0)
            millis = -millis;

        long years = 0;
        long months = 0;
        long days = 0L;
        long hours = 0L;
        long minutes = 0L;
        long seconds = 0L;

        // calculate days and months
        days = millis / (24 * 3600 * 1000);
        millis = millis - (days * 24 * 3600 * 1000);

        // calculate months
        months = (days - days % 30L) / 30L;
        if (months > 0)
            days = days % 30;

        // years
        years = (months - months % 12L) / 12L;
        if (years > 0)
            months = months % 12;

        hours = millis / (3600 * 1000);
        millis = millis - (hours * 3600 * 1000);

        minutes = millis / (60 * 1000);
        millis = millis - (minutes * 60 * 1000);

        seconds = millis / (1000);
        millis = millis - (seconds * 1000);

        StringBuilder sb = new StringBuilder(30);

        if (sign < 0)
            sb.append('-');

        if (years > 0) {
            sb.append(years + "y ");

            if (months > 0)
                sb.append(months + "mon ");

            if (days > 0)
                sb.append(days + "d ");
        } else if (months > 0) {
            sb.append(months + "mon ");

            if (days > 0)
                sb.append(days + "d ");

            if (hours > 0)
                sb.append(hours + "h ");

            if (minutes > 0)
                sb.append(minutes + "m ");
        } else {
            if (days > 0)
                sb.append(days + "d ");

            if (hours > 0)
                sb.append(hours + "h ");

            if (minutes > 0)
                sb.append(minutes + "m ");

            if (seconds > 0)
                sb.append(seconds + "s ");

            if (millis > 0)
                sb.append(millis + "ms ");
        }

        return sb.toString().trim();
    }

    /**
     * Split a string by a given character, into sub-strings (array list
     * format),
     * 
     * @param pInputString input string
     * @param pDelimiter   delimier
     * @return array list
     */
    public static List<String> splitToArrayList(String pInputString, char pDelimiter) {
        List<String> oList = new ArrayList<>();

        // split string based on charachers
        StringBuilder sb = new StringBuilder(pInputString);
        StringBuilder sbThisRow = new StringBuilder("");

        // String str = new String(pInputString);

        // if the string is empty, then retun an empty array list
        if (pInputString.length() > 0) {
            char c;

            for (int i = 0; i < sb.length(); i++) {
                c = sb.charAt(i);

                if (c == pDelimiter) {
                    oList.add((sbThisRow.toString()).trim());
                    sbThisRow.setLength(0);
                } else {
                    sbThisRow.append(c);
                }
            }

            oList.add((sbThisRow.toString()).trim());
        }

        return oList;
    }

}
