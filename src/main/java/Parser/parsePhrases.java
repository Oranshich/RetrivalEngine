package Parser;

public class parsePhrases extends AParser {
    @Override
    public void parse() {

    }
    @Override
    public void run() {
        System.out.println("Date Parser has started");
        while(!stopThread)
        {
            parse();
        }
        System.out.println("Parsed Numbers is stopped");

    }
}
