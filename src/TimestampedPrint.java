import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimestampedPrint extends PrintStream {

	private final SimpleDateFormat format;
	
	public TimestampedPrint(PrintStream out) {
		super(out);
		format = new SimpleDateFormat("HH:mm:ss");
	}
	
	@Override
	public void println(String x)
	{
		String timestamp = format.format(new Date());
		super.println(String.format("[%s] %s", timestamp, x));
	}

}
