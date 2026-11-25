import com.suriya.expensetracker.PayeeParser;
public class PayeeParserTest {
  static void check(String expected,String actual){if(!java.util.Objects.equals(expected,actual))throw new AssertionError("Expected "+expected+" but got "+actual);}
  public static void main(String[] args){
    check("DEMO CAFE",PayeeParser.extract("Your a/c XXXXX99 debited for payee DEMO CAFE for Rs. 180.00 on 2026-11-20, ref 123. If not you, report to your bank immediately-IOB."));
    check("Demo Person",PayeeParser.extract("Your a/c XXXXX99 debited for payee Demo\nPerson for Rs. 22.00 on 2026-11-22, ref 123."));
    check("Sample Shop",PayeeParser.extract("debited for payee Sample   Shop for INR 20 on 2026-01-01"));
    check(null,PayeeParser.extract("Your account was credited for payee Sample for Rs. 20"));
    check(null,PayeeParser.extract("Your account was debited for Rs. 20 at Sample"));
    check(null,PayeeParser.extract("debited for payee  for Rs. 22"));
    check("demo person",PayeeParser.normalize("  DEMO   PERSON "));
    System.out.println("Payee parser tests passed");
  }
}
