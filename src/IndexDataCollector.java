import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class IndexDataCollector {
    /*
     *   arguments to be used for the default
     *    -p Path where to save the results
     *    -C Company name to get the last result or list of companies with comma delimiter
     *    -I index for search if not defined use "TA-125"
     *    -a ascending sorting the companies included in the index by <String Column> <optional>
     *    -d descending sorting the companies included in the index by <String Column> <optional>
     *            if sorting direction not defined use -a if string not defined use "Turnover"
     *    -h - help
     */

    public  static Scanner in = new Scanner(System.in);

    static String path;
    static String companyName;
    static String companyHistoryPeriod;
    static String groupName;
    static boolean isAscending;
    static boolean isSilent;
    static String sortIndex;
    static String webDriverLocation;


    public static void setCompanyName(){
        System.out.println("Please input the company name to retrieve data for, " +
                "or type C for cancelling collecting the data for he specific company: ");
        String tCompany = in.nextLine().toUpperCase().trim();
        if (tCompany.equalsIgnoreCase("C")){
            companyName = null;
        }
        else{
            companyName = tCompany;
        }
    }

    public static void main(String[] args) {
        //read execution defaults from the configuration file
        String myDir = System.getProperty("user.dir");
        File configFile =  new File(myDir + File.separator + "src/IndexDataCollector.cfg");
        ArrayList <String[]> defaults = Outer.getConfigFromFile(configFile);
        for (String[] params: defaults){
            if (params[0].toUpperCase().contains("DRIVER")){
                if(Files.isExecutable(Paths.get(params[1].trim()))){
                    webDriverLocation = params[1].trim();
                }
            }
            else if (params[0].toUpperCase().contains("PATH")){
                path = params[1].trim();
            }
            else if (params[0].toUpperCase().contains("URL")){
                IndexData.url = params[1].trim();
            }
            else if (params[0].toUpperCase().contains("GROUP")){
                groupName = params[1].trim().toUpperCase();
            }
            else if (params[0].toUpperCase().contains("SORTINDEX")){
                sortIndex = params[1].trim();
            }
            else if (params[0].toUpperCase().contains("SORTDIRECTION")){
                isAscending = params[1].contains("ASC");
            }
            else if (params[0].toUpperCase().contains("PERIOD")){
                companyHistoryPeriod = params[1].toUpperCase().trim();
            }
            else if (params[0].toUpperCase().contains("COMPANYNAME")){
                companyName = params[1].trim().toUpperCase();
            }
            else if (params[0].toUpperCase().contains("SILENT")){
                isSilent = params[1].trim().equalsIgnoreCase("true");
            }
        }

        //read the arguments and update the execution defaults
        boolean isInvalid = false;

        for (int a = 0; a < args.length; a++){

            switch (args[a].toUpperCase()) {

                case "-0":{
                    a++;
                    if(Outer.isValidPath(args[a])){
                        path = args[a];
                    }
                    else {
                        isInvalid = true;
                        System.out.printf("The path argument is invalid: [%s]\n", args[a]);
                    }
                    break;
                }

                case "-P":{
                    a++;
                    if(Files.isExecutable(Paths.get(args[a]))){
                        webDriverLocation = args[a];
                    }
                    else{
                        isInvalid = true;
                        System.out.printf("The selenium web driver path is invalid: [%s]\n", args[a]);
                    }
                }

                case "-I":{
                    a++;
                    groupName = args[a].toUpperCase();
                    break;
                }

                case "-S":{
                    a++;
                    isSilent = true;
                    break;
                }

                case "-C":{
                    a++;
                    companyName = args[a].toUpperCase();
                    break;
                }

                case "-T":{
                    String[] allowedPeriod = new String[]{"1D", "1W", "1M", "3M", "1Y"};
                    List <String> period = Arrays.asList(allowedPeriod);
                    a++;
                    if (period.contains(args[a].toUpperCase())){
                        companyHistoryPeriod = args[a].toUpperCase();
                    }
                    else{
                        isInvalid = true;
                        System.out.printf("Illegal period argument [%s] \n", args[a]);
                        System.out.printf("The supported periods are: [%s] \n", Arrays.toString(allowedPeriod));
                    }
                    break;
                }

                case "-A":{
                    if (a != args.length - 1 && !args[a + 1].contains("-")){
                        a++;
                        sortIndex = args[a].toUpperCase();
                    }
                    isAscending = true;
                    break;
                }

                case "-D":{
                    if (a != args.length - 1 && !args[a + 1].contains("-")){
                        a++;
                        sortIndex = args[a].toUpperCase();
                    }
                    isAscending = false;
                    break;
                }

                case "-H":{
                    isInvalid = true;
                    System.out.println("Following arguments supported: ");
                    System.out.println("-o\tPath where to save the results");
                    System.out.println("-p\tPath where the selenium chrome driver located.");
                    System.out.println("\t\t**the default location is: C:/Selenium/Drivers/chromedriver.exe");
                    System.out.println("-c\tCompany name to get the last result or list of companies with comma delimiter");
                    System.out.println("-t\ttime period to present the company graph. If not defined 1Y period used");
                    System.out.println("\t\t** the supported periods are: (\"1D\", \"1W\", \"1M\", \"3M\", \"1Y\")");
                    System.out.println("-i\tindex for search if not defined use \"TA-125\"");
                    System.out.println("-a\tascending sorting the companies included in the index by <String Column> <optional>");
                    System.out.println("-d\tdescending sorting the companies included in the index by <String Column> <optional>");
                    System.out.println("\t\t** default sorting is \"descending by Turnover (NIS thousands)\"");
                    System.out.println("-s\tsilent execution, will throw an exception if any issue (not a default)");
                    System.out.println();
                    System.out.println("** Example:");
                    System.out.println("\tIndexDataCollector -p c:\\temp\\bursa -C \"CLAL INSURANCE\" -d \"Last Rate\"");
                    System.out.println("\tor IndexDataCollector -p c:\\temp\\bursa -C \"CLAL INSURANCE\" -d");
                }

                default:{
                    System.out.printf("Unsupported argument:\t[%s], use -h argument for help\n", args[a]);
                    break;
                }

            }
        }
        if (webDriverLocation == null){
            System.out.println("The Selenium Chromedriver location is missing. " +
                    "Please update it in the configuration file or supply it as running argument (-p path/to/driver/here)");
            System.exit(-1);
        }
        if (!isInvalid){
            if (path == null){ //if the path not defined ask from user to define this
                if (!isSilent){
                    while (true){
                        System.out.println("Please define the valid path to save the results: ");
                        String tPath = in.nextLine().trim();
                        if (Outer.isValidPath(tPath)){
                            path = tPath;
                            break;
                        }
                    }
                }
                else{
                    throw new IllegalArgumentException("The path to save the results did not supply");
                }

            }

            if (companyName == null){
                if (!isSilent){
                    setCompanyName();
                }
                else
                    System.out.println("The company name did not supply and the data collection will be cancelled");
            }

            //collecting the data and company details
            IndexData.dataCollector(groupName, sortIndex, isAscending, companyName);
            try{
                //retrieve the details for the selected company
                IndexData.companyDetailsGetter(IndexData.companyUrl);
            }catch (IllegalArgumentException ex){
                System.out.println("Exception occurred: " + ex);
                System.exit(-1);
            }
        }
    }
}
