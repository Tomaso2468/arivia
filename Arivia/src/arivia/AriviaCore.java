package arivia;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Locale;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

import ebot.Context;
import ebot.EBot;
import ebot.InputFilter;
import ebot.ListContext;
import ebot.ebf.EBFGroup;
import ebot.ebf.EBFIO;
import ebot.ebf.EBFText;
import ebot.ebf.EBFTree;
import ebot.ebs.EBSScript;
import ebot.ebs.WorldInterface;
import ebot.io.FileDataSource;
import ebot.text.ComplexTextComparer;

public class AriviaCore {
	public static final int NUM_RANGE = 100;

	private EBot bot;

	public void start(String urbanDictionaryKey) throws IOException {
		System.out.println("Loading tree.");

		EBFTree tree = EBFTree.parse(new FileDataSource("arivia.ebf"));
		System.out.println(tree);

		loadAddition(tree);
		loadSubtraction(tree);
		loadMultiply(tree);
		loadDivide(tree);
		loadDefinitions(tree);
		loadSquareSize(tree);

		System.out.println("Loading memory.");
		if (new File("arivia.ebi").exists()) {
			tree.loadEBI(new FileDataSource("arivia.ebi"));
		}

		System.out.println("Loading bot.");

		bot = new EBot(tree, new ComplexTextComparer() {
			@Override
			public double compare(String a, String b) {
				if (a.toLowerCase().startsWith("define ") && b.toLowerCase().startsWith("define ")) {
					String[] split = a.toLowerCase().split(" ");
					String[] split2 = b.toLowerCase().split(" ");
					if (split[1].equals(split2[1])) {
						return Double.MAX_VALUE;
					}
					return 100 / calculate(split[1], split2[1]);
				}
				return super.compare(a, b);
			}
		}) {
			@Override
			public void doTask(String d, WorldInterface wi, Context c) {
				if (d.toLowerCase().startsWith("define ")) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					try {
						URL url = new URL("https://mashape-community-urban-dictionary.p.rapidapi.com/define?term=" + d.toLowerCase().substring(d.toLowerCase().indexOf(' ') + 1).replace("&", "%26").replace(" ", "%20"));
						HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
						con.setRequestMethod("GET");

						con.addRequestProperty("x-rapidapi-host", "mashape-community-urban-dictionary.p.rapidapi.com");
						con.addRequestProperty("x-rapidapi-key", urbanDictionaryKey);

						con.setConnectTimeout(5000);
						con.setReadTimeout(5000);

						con.connect();
						
						System.out.println("Request: " + con.getResponseCode());
						
						BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
						String inputLine;
						StringBuffer content = new StringBuffer();
						while ((inputLine = in.readLine()) != null) {
							content.append(inputLine);
						}
						in.close();
						
						con.disconnect();
						
						System.out.println(content);
						
						JSONObject obj = new JSONObject(content.toString());
						
						System.out.println(obj);
						
						JSONArray array = obj.getJSONArray("list");
						String s = array.getJSONObject((int) (Math.random() * array.length() - 1)).getString("definition");
						
						wi.out(d.toLowerCase().substring(d.toLowerCase().indexOf(' ') + 1) + ": " + s);
						
						return;
					} catch (Exception e) {
						e.printStackTrace();
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
				}
				super.doTask(d, wi, c);
			}
		};

//		bot.addTextProcessor("sarcasm", SarcasmDetector.load(24, 5, new FileDataSource("sarcasmP.txt"),
//				new FileDataSource("sarcasmN.txt"), 0.9, 1000));

		bot.addFilter(new InputFilter() {
			@Override
			public boolean check(String in) {
				boolean b = false;
				if (in.toLowerCase().contains("sum")) {
					b = true;
				}
				if (in.toLowerCase().contains("+")) {
					b = true;
				}
				if (in.toLowerCase().contains("add")) {
					b = true;
				}
				if (in.toLowerCase().contains("plus")) {
					b = true;
				}
				if (in.toLowerCase().contains("-")) {
					b = true;
				}
				if (in.toLowerCase().contains("minus")) {
					b = true;
				}
				if (in.toLowerCase().contains("take away")) {
					b = true;
				}
				if (in.toLowerCase().contains("subtract")) {
					b = true;
				}

				return b;
			}

			@Override
			public String getGroup() {
				return "maths_addsub";
			}
		});
		bot.addFilter(new InputFilter() {
			@Override
			public boolean check(String in) {
				boolean b = false;
				if (in.toLowerCase().contains("*")) {
					b = true;
				}
				if (in.toLowerCase().contains("times")) {
					b = true;
				}
				if (in.toLowerCase().contains("multiplied by")) {
					b = true;
				}
				if (in.toLowerCase().contains("product")) {
					b = true;
				}

				return b;
			}

			@Override
			public String getGroup() {
				return "maths_multiply";
			}
		});
		bot.addFilter(new InputFilter() {
			@Override
			public boolean check(String in) {
				boolean b = false;
				if (in.toLowerCase().contains("/")) {
					b = true;
				}
				if (in.toLowerCase().contains("divide")) {
					b = true;
				}
				if (in.toLowerCase().contains("divided")) {
					b = true;
				}

				return b;
			}

			@Override
			public String getGroup() {
				return "maths_divide";
			}
		});
		bot.addFilter(new InputFilter() {
			@Override
			public boolean check(String in) {
				return in.toLowerCase().contains("define");
			}

			@Override
			public String getGroup() {
				return "define";
			}
		});
		bot.addFilter(new InputFilter() {
			@Override
			public boolean check(String in) {
				return in.toLowerCase().contains("square") && in.toLowerCase().contains("area");
			}

			@Override
			public String getGroup() {
				return "maths_square";
			}
		});

		bot.load();

		new Thread("backgroundThread") {
			public void run() {
				System.out.println("Starting background thread.");
				bot.startBackground(60000, 2000);
			}
		}.start();

		System.gc();
	}

	public void loadSquareSize(EBFTree tree) {
		EBFGroup g = new EBFGroup("f:maths_square");
		tree.root.getSubGroups().add(g);
		loadSquareSizeInches(g, NUM_RANGE);
	}

	public void loadSquareSizeInches(EBFGroup g, int range) {
		for (int x = -range / 2; x < +range; x++) {
			EBFIO io = new EBFIO(
					new EBFText[] { new EBFText("What is the area of a square with a length of " + x + " inches?") },
					new EBFText[] { new EBFText("A square of " + x + " inches is " + (x * x) + " inches squared."), },
					new EBFText[0], new EBFText[0], new EBSScript[0]);

			g.getIOs().add(io);
		}
	}

	public void loadDefinitions(EBFTree tree) throws IOException {
		System.out.println("Loading definitions.");

		EBFGroup g = new EBFGroup("f:define");
		tree.root.getSubGroups().add(g);

		Scanner s = new Scanner(new FileDataSource("wordsUnsafe").openStream());
		while (s.hasNextLine()) {
			String line = s.nextLine().trim();

			if (line.isEmpty()) {
				continue;
			}

			String word = line.split(" ")[0];

			EBFIO io = new EBFIO(new EBFText[] { new EBFText("Define " + word) }, new EBFText[] { new EBFText(line), },
					new EBFText[0], new EBFText[0], new EBSScript[0]);
			g.getIOs().add(io);
		}
		s.close();
	}

	public void loadAddition(EBFTree tree) {
		EBFGroup g = new EBFGroup("f:maths_addsub");
		tree.root.getSubGroups().add(g);
		loadAddition(g, NUM_RANGE);
	}

	public void loadAddition(EBFGroup g, int range) {
		System.out.println("Loading addition module");

		for (int x = -range / 2; x < +range; x++) {
			for (int y = -range / 2; y < +range; y++) {
				EBFIO io = new EBFIO(
						new EBFText[] { new EBFText(x + "+" + y), new EBFText(x + " + " + y),
								new EBFText(x + " plus " + y), new EBFText(x + " add " + y),
								new EBFText("What is " + x + " + " + y), new EBFText("What is " + x + " plus " + y),
								new EBFText("What is " + x + " add " + y),
								new EBFText("What is the sum of " + x + " and " + y) },
						new EBFText[] { new EBFText(x + " + " + y + " = " + (x + y)),
								new EBFText("The answer is " + (x + y)), new EBFText((x + y) + ""), },
						new EBFText[0], new EBFText[0], new EBSScript[0]);

				g.getIOs().add(io);
			}
		}

		System.out.println("Loading linear nth term module");
		for (int x = -5; x <= 5; x++) {
			for (int y = -10; y <= 10; y++) {
				String s = (x == 1 ? "" : x) + "n";
				s += (y < 0) ? "" : "+";
				s += y;
				s += ".";

				String s2 = "";
				for (int i = 1; i <= 5; i++) {
					s2 += (x * i + y) + ", ";
				}

				EBFIO io = new EBFIO(new EBFText[] { new EBFText("Write out " + s) },
						new EBFText[] { new EBFText(s2), }, new EBFText[0], new EBFText[0], new EBSScript[0]);

				g.getIOs().add(io);

				EBFIO io2 = new EBFIO(new EBFText[] { new EBFText("What is the nth term of " + s2) },
						new EBFText[] { new EBFText(s), }, new EBFText[0], new EBFText[0], new EBSScript[0]);

				g.getIOs().add(io2);
			}
		}

		System.out.println("Loading square nth term module");
		for (int x = -2; x <= 2; x++) {
			for (int y = -5; y <= 5; y++) {
				for (int z = -10; z <= 10; z++) {
					String s = (x == 1 ? "" : x) + "n^2";
					s += (y < 0) ? "" : "+";
					s += (y == 1 ? "" : y) + "n";
					s += (z < 0) ? "" : "+";
					s += z;
					s += ".";

					String s2 = "";
					for (int i = 1; i <= 5; i++) {
						s2 += (x * x * i + y * i + z) + ", ";
					}

					EBFIO io = new EBFIO(new EBFText[] { new EBFText("Write out " + s) },
							new EBFText[] { new EBFText(s2), }, new EBFText[0], new EBFText[0], new EBSScript[0]);

					g.getIOs().add(io);

					EBFIO io2 = new EBFIO(new EBFText[] { new EBFText("What is the nth term of " + s2) },
							new EBFText[] { new EBFText(s), }, new EBFText[0], new EBFText[0], new EBSScript[0]);

					g.getIOs().add(io2);
				}
			}
		}
	}

	public void loadSubtraction(EBFTree tree) {
		EBFGroup g = new EBFGroup("f:maths_addsub");
		tree.root.getSubGroups().add(g);
		loadSubtraction(g, NUM_RANGE);
	}

	public void loadSubtraction(EBFGroup g, int range) {
		System.out.println("Loading subtraction module");

		for (int x = -range / 2; x < +range; x++) {
			for (int y = -range / 2; y < +range; y++) {
				EBFIO io = new EBFIO(new EBFText[] { new EBFText(x + "-" + y), new EBFText(x + " - " + y),
						new EBFText(x + " minus " + y), new EBFText(x + " subtract " + y),
						new EBFText(x + " take away " + y), new EBFText("What is " + x + " - " + y),
						new EBFText("What is " + x + " minus " + y), new EBFText("What is " + x + " subtract " + y),
						new EBFText("What is " + x + " take away " + y), },
						new EBFText[] { new EBFText(x + " - " + y + " = " + (x - y)),
								new EBFText("The answer is " + (x - y)), new EBFText((x - y) + ""), },
						new EBFText[0], new EBFText[0], new EBSScript[0]);

				g.getIOs().add(io);
			}
		}
	}

	public void loadMultiply(EBFTree tree) {
		EBFGroup g = new EBFGroup("f:maths_multiply");
		tree.root.getSubGroups().add(g);
		loadMultiply(g, NUM_RANGE);
	}

	public void loadMultiply(EBFGroup g, int range) {
		System.out.println("Loading multiply module");

		for (int x = -range / 2; x < +range; x++) {
			for (int y = -range / 2; y < +range; y++) {
				EBFIO io = new EBFIO(
						new EBFText[] { new EBFText(x + "*" + y), new EBFText(x + " * " + y),
								new EBFText(x + " times " + y), new EBFText(x + " multiplied by " + y),
								new EBFText("What is " + x + " * " + y), new EBFText("What is " + x + " times " + y),
								new EBFText("What is " + x + " multiplied by " + y),
								new EBFText("What is the product of " + x + " and " + y) },
						new EBFText[] { new EBFText(x + " * " + y + " = " + (x * y)),
								new EBFText("The answer is " + (x * y)), new EBFText((x * y) + ""), },
						new EBFText[0], new EBFText[0], new EBSScript[0]);

				g.getIOs().add(io);
			}
		}
	}

	public void loadDivide(EBFTree tree) {
		EBFGroup g = new EBFGroup("f:maths_divide");
		tree.root.getSubGroups().add(g);
		loadDivide(g, NUM_RANGE);
	}

	public void loadDivide(EBFGroup g, int range) {
		System.out.println("Loading divide module");

		for (int x = 0; x < +range; x++) {
			for (int y = 0; y < +range; y++) {
				EBFIO io = new EBFIO(
						new EBFText[] { new EBFText(x + "/" + y), new EBFText(x + " / " + y),
								new EBFText(x + " divided by " + y), new EBFText("What is " + x + " / " + y),
								new EBFText("What is " + x + " divided by " + y), },
						new EBFText[] { new EBFText(x + " / " + y + " = " + ((double) x / y)),
								new EBFText("The answer is " + ((double) x / y)), new EBFText(((double) x / y) + ""), },
						new EBFText[0], new EBFText[0], new EBSScript[0]);

				g.getIOs().add(io);
			}
		}
	}

	public void acceptConnection(Connection cn) {
		WorldInterface wi = new WorldInterface() {
			@Override
			public Locale getLocale() {
				return cn.getLocale();
			}

			@Override
			public void couldNotFind() {
				out("I am sorry. I do not know what you mean.");
			}

			@Override
			public void out(String s) {
				cn.out(s);
			}
		};

		Context c = new ListContext(cn.getUserName());

		bot.onConnect(wi, c);

		cn.prompt();

		while (true) {
			while (!cn.hasNextLine()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			String line = cn.nextLine().trim();

			bot.doTask(line, wi, c);

			cn.prompt();
		}
	}

	public static void main(String[] args) throws IOException {
		AriviaCore core = new AriviaCore();

		core.start(args[0]);

		core.acceptConnection(new LocalConnection(new Scanner(System.in), System.out, System.getProperty("user.name"),
				Locale.ENGLISH));

		System.exit(0);
	}

}
