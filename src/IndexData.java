import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;


public class IndexData {
    static WebDriver driver;
    static String url;
    static String companyUrl;


    public static boolean isNumeric(String strNum) {
        // the function returns true if supplied string is a number, and false if it does not.
        if (strNum == null || strNum.equals("")) {
            return false;
        }
        try {
            Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }


    public void invokeBrowser(String currentUrl, String webDriver) {
        /* The Function receives the URL to open the browser and the string with the selenium chrome driver location
        * the function defines the defaults for the screen removes cookies and defines the timeouts
        */
        try {
            System.setProperty("webdriver.chrome.driver",webDriver);
            driver = new ChromeDriver();
            driver.manage().window().maximize();
            driver.manage().deleteAllCookies();
            driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
            driver.manage().timeouts().pageLoadTimeout(40, TimeUnit.SECONDS);
            driver.get(currentUrl);
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
            driver.close();
            System.exit(-1);
        }
    }


    public int getIndexByText(String actualXpath, String indexText, boolean isHeader){
        /* The function receives the parameters:
         *      actualXpath (String) - to get the elements inside and look for the
         *      indexText (String) in the elements text.
         *         if text includes number of lines
         *      isHeader (boolean) should be true (for example header the text will be divided to two and
         *         the second element will be compared with the
         *         indexText).
         *      The Function return the index (int) in the
         *         "one base" (suitable for using in the xpath)
         *      throws the IllegalArgumentException if the text did not appear in the elements text
         */

        List<WebElement> indexes = driver.findElements(By.xpath(actualXpath));
        int i = 0;
        boolean exists = false;
        while (i < indexes.size()){
            String actualText;
            if (isHeader){
                String[] index = indexes.get(i).getText().split("\n");
                actualText = index[1];
            }
            else {
                actualText = indexes.get(i).getText();
            }

            if (actualText.equalsIgnoreCase(indexText)){
                exists = true;
                break;
            }
            else {
                i++;
            }
        }
        if(!exists){
            throw new IllegalArgumentException(String.format("The text %s does not appear in the source by xPath [%s]",
                                                                indexText, actualXpath));
        }
        return i + 1; // convert Zero base to one bse used by xpath
    }


    public static void dataCollector(String index, String sortBy, boolean isAscending, String companyName) {
        String fileName = Outer.stringDateTimeAdd(index);

        IndexData currentIndex = new IndexData();

        currentIndex.invokeBrowser(url, IndexDataCollector.webDriverLocation);

        /*  looking the group name in the table first cell in each row by its text.
            When Found the index use it to create the relevant xpath to link.
            Click on the index link
        */
        try {
            int i = currentIndex.getIndexByText("//*[@id='trades_panel1']//table//tbody/tr/td[1]", index, true);
            String tmp_xpath = String.format("//*[@id='trades_panel1']//table//tbody/tr[%d]//a",i);
             //Navigate to the Index Details page
            driver.findElement(By.xpath(tmp_xpath)).click();
        }catch (IllegalArgumentException e){
            System.out.printf("The Index [%s] did not find in the index table\n", index);
            driver.close();
            System.exit(-1);
        }

        //Find the index of the Market Data in the links and press on the link
        try {
            int i = currentIndex.getIndexByText("//*[@id='indexMostActive']/div[@class='madad_links_zone clearfix']/a",
                    "Market Data", false);
            String tmp_xpath = String.format("//*[@id='indexMostActive']/div[@class='madad_links_zone clearfix']/a[%d]", i);
            //Navigate to the companies included.
            driver.findElement(By.xpath(tmp_xpath)).click();
        }catch (IllegalArgumentException e){
            System.out.printf("Can't click on Market Data link for opening the index [%s] details\n", index);
            driver.close();
            System.exit(-1);
        }

        // Sort the table by column / direction
        List<WebElement> sortButtons = driver.findElements(By.xpath("//*[@id=\"mainContent\"]//tr[@class=\"sort-btns\"]//td//*[@type=\"button\"]"));
        String sortDirection = "descending";
        if (isAscending){
            sortDirection = "ascending";
        }
        for (WebElement button: sortButtons){
            if (button.getAttribute("aria-label").toLowerCase().contains(sortDirection) &&
                button.getAttribute("aria-label").toLowerCase().contains(sortBy.toLowerCase())){
                    button.click();
            }
        }

        //get headers to line
        int totalCols = 0;
        File dataFile = new  File(IndexDataCollector.path + File.separator + fileName + ".txt");
        Outer.createFile(dataFile);
        String tXpath = "//div[@class=\"table_page_table_container\"]//thead//th";
        try {
            List<WebElement> headers = driver.findElements(By.xpath(tXpath));
            totalCols = headers.size(); //size = 9
            LinkedList<String> th = new LinkedList<>();
            for (WebElement header : headers) {
                th.add(header.getText());
            }
            String fileHeader = String.join(",", th);
            Outer.appendLineToFile(dataFile, fileHeader);

        }catch (Exception e){
            System.out.printf("can't find the header elements by xpath: [%s]", tXpath);
            driver.close();
            System.exit(-1);
        }

        //Get pages count and there indexes to list
        List<WebElement> pages = driver.findElements(By.xpath("//index-market-data//pagination-controls//li"));
        LinkedList<Integer> pageIndex = new LinkedList <>();
        for (int i = 0; i < pages.size(); i++){
            String[] tPage = pages.get(i).getText().split("\n");
            if (tPage.length > 1 && isNumeric(tPage[1])){
                pageIndex.add(i + 1);
            }
        }

        //get elements from all pages

         for(Integer page: pageIndex){  // select every page in pages
            String pageNumXpath = String.format("//index-market-data//pagination-controls//li[%d]",page);
            driver.findElement(By.xpath(pageNumXpath)).click(); // go to the page
            List<WebElement> rows = driver.findElements(By.xpath("//div[@class=\"table_page_table_container\"]//tbody//tr"));

            for (int r = 0; r < rows.size(); r++){ //for each row in one base
                ArrayList<String> rowData = new ArrayList<>(); //define empty row
                for (int c = 0; c < totalCols; c++){ //for each column in one base --> get cell data
                    tXpath = String.format("//div[@class=\"table_page_table_container\"]//tbody//tr[%d]/td[%d]",r+1, c+1);

                    //Performance issue here. Accessing to the element takes a long time.
                    String elementText = driver.findElement(By.xpath(tXpath)).getText();
                    String[] cell = elementText.replace(",","").split("\n"); //remove the group delimiter comma from numbers

                    // If Company name is equal to the given one - keep its url
                    if (c == 0 && cell[0].equalsIgnoreCase(companyName)){
                        tXpath += "/a";
                        IndexData.companyUrl = driver.findElement(By.xpath(tXpath)).getAttribute("href");
                    }

                    rowData.add(cell[0]);
                }
                Outer.appendLineToFile(dataFile, String.join(",", rowData));
            }
        }
        driver.close();
    }

    public static void companyDetailsGetter(String url){
        /*  Verify if we have the link to the selected company page,
            if not (url string is null), so the company name did not provide,
            or the  provided company is not in the provided group
        */
        if (IndexData.companyUrl == null){
            if (IndexDataCollector.isSilent){
                 if (IndexDataCollector.companyName == null){
                     System.out.println("The Company name did not provide and the collecting the company data cancelled.");
                 }
                 else {
                     throw new IllegalArgumentException("The provided company is not part of the provided group");
                 }
            }
            else{
                // If not in silent mode ask to enter the company name and looking for the relevant URL
                while (true){
                    System.out.printf("The provided company name \"%s\" is not in the Index \"%s\".\n" +
                                    "Please enter the company name or press C for cancel\n",
                            IndexDataCollector.companyName, IndexDataCollector.groupName);
                    //Update the company Name
                    IndexDataCollector.setCompanyName();
                    // If not supplied - cancel the data retrieving
                    if (IndexDataCollector.companyName == null){
                        break;
                    }
                    //if supplied bring the url to the company page
                    setCompanyUrl(IndexDataCollector.companyName, IndexDataCollector.groupName);
                    if (companyUrl != null){
                        url = companyUrl;
                        break;
                    }
                }
            }
        }
        if (url == null){
            System.out.println("The correct Company name did not provide. The collecting Company details cancelled");
        }
        else{
            String fileName = Outer.stringDateTimeAdd(IndexDataCollector.companyName);
            IndexData companyIndex = new IndexData();

            companyIndex.invokeBrowser(url, IndexDataCollector.webDriverLocation);
            //go over the periods on the graph and when it is equal to wanted one press on it.
            List<WebElement> periods = driver.findElements(By.xpath("//*[@id=\"mainContent\"]//chart-period-menu//li"));
            for (WebElement period: periods){
                if (period.getText().equalsIgnoreCase(IndexDataCollector.companyHistoryPeriod)){
                    period.click();
                }
            }
            try{
                //Sleep for page refreshing
                Thread.sleep(3000);
            }catch (InterruptedException e){
                e.printStackTrace();
                driver.close();
                System.exit(-1);
            }
            //Preparing the screenshot of the page
            File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            try{
                Outer.copyFile(scrFile, new File(IndexDataCollector.path + File.separator + fileName + ".png"));
            }catch (IOException e){
                System.out.printf("Exception occurred while copying the file: %s to: %s\n",
                        scrFile, (IndexDataCollector.path + File.separator + fileName + ".png"));
                e.printStackTrace();
                driver.close();
                System.exit(-1);
            }
            driver.close();
        }
    }

    public static void setCompanyUrl(String companyName, String groupName){

        IndexData id = new IndexData();
        id.invokeBrowser(url, IndexDataCollector.webDriverLocation);
         // go to the group page by clicking on the relevant link according to the group name
        try {
            int i = id.getIndexByText("//*[@id='trades_panel1']//table//tbody/tr/td[1]", groupName, true);
            String tmp_xpath = String.format("//*[@id='trades_panel1']//table//tbody/tr[%d]//a",i);
             //Navigate to the Index Details page
            driver.findElement(By.xpath(tmp_xpath)).click();
        }catch (IllegalArgumentException e) {
            System.out.printf("The Index [%s] did not find in the index table\n", groupName);
            driver.close();
            System.exit(-1);
        }

        //Press on the Market data link to open the companies list in the group details page
        try {
            int i = id.getIndexByText("//*[@id='indexMostActive']/div[@class='madad_links_zone clearfix']/a",
                    "Market Data", false);
            String tmp_xpath = String.format("//*[@id='indexMostActive']/div[@class='madad_links_zone clearfix']/a[%d]", i);
            //Navigate to the companies included.
            driver.findElement(By.xpath(tmp_xpath)).click();
        }catch (IllegalArgumentException e){
            System.out.printf("Can't click on Market Data link for opening the index [%s] details\n", groupName);
            driver.close();
            System.exit(-1);
        }

        //Get pages count and there indexes to list
        List<WebElement> pages = driver.findElements(By.xpath("//index-market-data//pagination-controls//li"));
        LinkedList<Integer> pageIndex = new LinkedList <>();
        for (int i = 0; i < pages.size(); i++){
            String[] tpage = pages.get(i).getText().split("\n");
            if (tpage.length > 1 && isNumeric(tpage[1])){
                pageIndex.add(i + 1);
            }
        }

        // looking for the company in the table
        for(Integer page: pageIndex){  // select every page in pages
            String pageNumXpath = String.format("//index-market-data//pagination-controls//li[%d]",page);
            driver.findElement(By.xpath(pageNumXpath)).click(); // go to the page
            List<WebElement> rows = driver.findElements(By.xpath("//div[@class=\"table_page_table_container\"]//tbody//tr"));

            for (int r = 1; r <= rows.size();r++){ //for each row in one base
                String tXpath = String.format("//div[@class=\"table_page_table_container\"]//tbody//tr[%d]/td[1]",r);
                String[] cell = driver.findElement(By.xpath(tXpath)).getText().replace(",","").split("\n");

                // If Company name is equal to the given one - keep its url
                if (cell[0].equalsIgnoreCase(companyName)){
                    tXpath += "/a";
                    companyUrl = driver.findElement(By.xpath(tXpath)).getAttribute("href");
                }
            }
        }
        driver.close();
    }
}
