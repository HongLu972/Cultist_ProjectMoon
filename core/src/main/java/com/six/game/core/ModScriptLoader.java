package com.six.game.core;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 模组脚本加载器：扫描 mods/&lt;模组&gt;/scripts/ 下的 .jar 与 .java。
 * <ul>
 *   <li>.jar 直接进类路径，枚举实现 {@link ModScript} 的类并回调；</li>
 *   <li>.java 需要运行环境带 JDK 编译器（javax.tools），编译到临时目录后同样加载；JRE 环境自动跳过并提示。</li>
 * </ul>
 * 单个脚本抛异常只记日志，不影响游戏与其它脚本。
 */
public final class ModScriptLoader {
    private ModScriptLoader() {}

    /** 所有 JSON 与校验完成后调用：扫描 mods 目录下各模组的 scripts 子目录并执行找到的脚本。 */
    public static void runScripts(Library library, EventBook events, GameConfig config) {
        File modsDir = new File("mods");
        if (!modsDir.isDirectory()) modsDir = new File("../mods");
        if (!modsDir.isDirectory()) return;

        List<File> jars = new ArrayList<>();
        List<File> classDirs = new ArrayList<>();
        File[] mods = modsDir.listFiles();
        if (mods == null) return;
        for (File mod : mods) {
            if (!mod.isDirectory()) continue;
            File scripts = new File(mod, "scripts");
            File[] files = scripts.listFiles();
            if (files == null) continue;
            List<File> sources = new ArrayList<>();
            for (File f : files) {
                String n = f.getName().toLowerCase();
                if (n.endsWith(".jar")) jars.add(f);
                else if (n.endsWith(".java")) sources.add(f);
            }
            if (!sources.isEmpty()) {
                File out = compileJava(sources);
                if (out != null) classDirs.add(out);
            }
        }

        int ran = 0;
        for (File jar : jars) {
            try (URLClassLoader cl = new URLClassLoader(new URL[]{jar.toURI().toURL()}, ModScriptLoader.class.getClassLoader())) {
                ran += scanJar(jar, cl, library, events, config);
            } catch (Throwable t) {
                System.out.println("[ModScript] 脚本 jar 处理失败: " + jar.getName() + " → " + t);
            }
        }
        for (File dir : classDirs) {
            try (URLClassLoader cl = new URLClassLoader(new URL[]{dir.toURI().toURL()}, ModScriptLoader.class.getClassLoader())) {
                ran += scanDir(dir, dir, cl, library, events, config);
            } catch (Throwable t) {
                System.out.println("[ModScript] 编译脚本处理失败 → " + t);
            }
        }
        if (ran > 0) System.out.println("[ModScript] 共执行 " + ran + " 个模组脚本喵");
    }

    private static int scanJar(File jar, ClassLoader cl, Library l, EventBook e, GameConfig c) {
        int ran = 0;
        try (JarFile jf = new JarFile(jar)) {
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry je = en.nextElement();
                String n = je.getName();
                if (n.endsWith(".class") && !n.contains("module-info") && !n.contains("package-info")) {
                    ran += tryRun(n.substring(0, n.length() - 6).replace('/', '.'), cl, l, e, c);
                }
            }
        } catch (Throwable t) {
            System.out.println("[ModScript] 无法读取 jar: " + jar.getName() + " → " + t);
        }
        return ran;
    }

    private static int scanDir(File base, File dir, ClassLoader cl, Library l, EventBook e, GameConfig c) {
        int ran = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            if (f.isDirectory()) {
                ran += scanDir(base, f, cl, l, e, c);
            } else if (f.getName().endsWith(".class")) {
                String rel = base.toURI().relativize(f.toURI()).getPath();
                ran += tryRun(rel.substring(0, rel.length() - 6).replace('/', '.'), cl, l, e, c);
            }
        }
        return ran;
    }

    private static int tryRun(String className, ClassLoader cl, Library l, EventBook e, GameConfig c) {
        try {
            Class<?> cls = Class.forName(className, false, cl);
            if (cls.isInterface() || cls.isMemberClass() || cls.isAnonymousClass() || cls.isLocalClass()) return 0;
            if (!ModScript.class.isAssignableFrom(cls)) return 0;
            Object script = cls.getDeclaredConstructor().newInstance();
            ((ModScript) script).onContentLoaded(l, e, c);
            System.out.println("[ModScript] 已执行: " + className);
            return 1;
        } catch (ClassNotFoundException | NoClassDefFoundError missing) {
            return 0; // 普通 jar 里的无关类或缺依赖，静默跳过
        } catch (Throwable t) {
            System.out.println("[ModScript] 脚本 " + className + " 执行失败 → " + t);
            return 0;
        }
    }

    /** 用 JDK 内置编译器把 .java 编到临时目录；JRE 环境返回 null 并提示（请改用 .jar）。 */
    private static File compileJava(List<File> sources) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.out.println("[ModScript] 当前是 JRE，没有内置编译器，跳过 .java 脚本（请改用 .jar）喵");
            return null;
        }
        try {
            File out = Files.createTempDirectory("modscripts").toFile();
            List<String> args = new ArrayList<>();
            args.add("-d");
            args.add(out.getAbsolutePath());
            args.add("-encoding");
            args.add("UTF-8");
            args.add("-nowarn");
            for (File f : sources) args.add(f.getAbsolutePath());
            int code = compiler.run(null, null, null, args.toArray(new String[0]));
            if (code != 0) {
                System.out.println("[ModScript] .java 编译失败（共 " + sources.size() + " 个文件），详情见上方编译输出喵");
                return null;
            }
            return out;
        } catch (Throwable t) {
            System.out.println("[ModScript] .java 编译异常 → " + t);
            return null;
        }
    }
}
