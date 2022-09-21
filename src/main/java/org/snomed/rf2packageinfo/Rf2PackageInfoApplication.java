package org.snomed.rf2packageinfo;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Rf2PackageInfoApplication {

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Expecting one argument which is the path to an RF2 archive.");
			System.exit(1);
		}
		String rf2File = args[0];
		File file = new File(rf2File);
		if (!file.isFile()) {
			System.err.println("Not a file: " + file.getAbsolutePath());
			System.exit(1);
		}

		new Rf2PackageInfoApplication().run(file);
	}

	private void run(File file) throws IOException {
		Date start = new Date();
		Map<String, AtomicLong> packageRowCounts = countPackages(file);
		System.out.println();
		System.out.println("Modules in package with row counts:");
		System.out.println(packageRowCounts);
		System.out.println();
		System.out.println("Took " + (((new Date().getTime() - start.getTime()) / 100)/10f) + " seconds");
		System.exit(0);
	}

	private Map<String, AtomicLong> countPackages(File rf2ZipFile) throws IOException {
		Map<String, AtomicLong> packageCounts = new HashMap<>();
		Set<String> moduleDependencies = null;
		try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(rf2ZipFile))) {
			ZipEntry nextEntry;
			while ((nextEntry = zipIn.getNextEntry()) != null) {
				String entryName = nextEntry.getName();
				if (!nextEntry.isDirectory() && entryName.contains("Snapshot") && entryName.endsWith(".txt")) {
					if (entryName.contains("der2_ssRefset_ModuleDependencySnapshot")) {
						moduleDependencies = readModuleDependencies(zipIn);
					} else {
						readZipEntry(zipIn, entryName, packageCounts);
					}
				}
			}
		}
		System.out.println();
		if (moduleDependencies != null) {
			for (String moduleDependency : moduleDependencies) {
				System.out.println("Module " + moduleDependency + " is required and is " + (packageCounts.containsKey(moduleDependency) ? "present." : "absent."));
			}
		} else {
			System.err.println("Module dependency file not found.");
		}
		return packageCounts;
	}

	private Set<String> readModuleDependencies(InputStream contentStream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(contentStream));
		String line = reader.readLine();
		// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	sourceEffectiveTime	targetEffectiveTime
		// 0	1				2		3			4			5						6					7
		Set<String> moduleDependencies = new HashSet<>();
		System.out.println();
		System.out.println("Module Dependencies:");
		while ((line = reader.readLine()) != null) {
			String[] values = line.split("\\t");
			if (values[2].equals("1")) {
				System.out.println(values[3] + " @ " + values[6] + " -> " + values[5] + " @ " + values[7]);
				moduleDependencies.add(values[5]);
			}
		}
		System.out.println();
		return moduleDependencies;
	}

	private void readZipEntry(InputStream contentStream, String name, Map<String, AtomicLong> packageCounts) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(contentStream));
		String line = reader.readLine();
		if (line == null || !line.startsWith("id\teffectiveTime\tactive\tmoduleId\t")) {
			if (!name.contains("/sct2_Identifier_")) {
				System.out.println("Skipping zip entry " + name);
			}
			return;
		}
		while ((line = reader.readLine()) != null) {
			String[] split = line.split("\\t", 5);
			packageCounts.computeIfAbsent(split[3], i -> new AtomicLong()).incrementAndGet();
		}
		System.out.print(".");
	}
}
