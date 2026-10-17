import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prints overall LINE coverage from a JaCoCo XML report for CI log parsing.
 * Uses the report-level counter (after the last package element).
 */
public final class PrintJacocoLineCoverage
{
	private static final Pattern LINE_COUNTER = Pattern.compile("type=\"LINE\" missed=\"(\\d+)\" covered=\"(\\d+)\"");

	private PrintJacocoLineCoverage()
	{
	}

	public static void main(String[] args) throws Exception
	{
		if (args.length != 1)
		{
			System.err.println("Usage: PrintJacocoLineCoverage <jacoco.xml>");
			System.exit(2);
		}

		final Path report = Path.of(args[0]);
		if (!Files.isRegularFile(report))
		{
			System.err.println("JaCoCo XML report not found: " + report);
			System.exit(1);
		}

		final String xml = Files.readString(report, StandardCharsets.UTF_8);
		final int afterPackages = xml.lastIndexOf("</package>");
		final String scope = afterPackages >= 0 ? xml.substring(afterPackages) : xml;
		final Matcher matcher = LINE_COUNTER.matcher(scope);
		if (!matcher.find())
		{
			System.out.println("Total line coverage: 0.0%");
			return;
		}

		final int missed = Integer.parseInt(matcher.group(1));
		final int covered = Integer.parseInt(matcher.group(2));
		final int total = missed + covered;
		final double percent = total == 0 ? 0.0 : (100.0 * covered) / total;
		System.out.printf(Locale.US, "Total line coverage: %.1f%%%n", percent);
	}
}
