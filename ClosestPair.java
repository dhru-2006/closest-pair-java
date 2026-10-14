import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Java 11+; no external libraries. Run from this file's folder:
 * java ClosestPair.java
 * Enter N. Open closest_pair_report.html after completion.
 */
public class ClosestPair {
    static class Point {
        final double x, y;
        final int id;
        Point(double x, double y, int id) { this.x=x; this.y=y; this.id=id; }
    }
    static class Result {
        final double distance;
        final Point a,b;
        Result(double d,Point a,Point b){distance=d;this.a=a;this.b=b;}
    }
    static class Timed {
        Result result; double ms;
        Timed(Result r,double ms){result=r;this.ms=ms;}
    }
    static final Comparator<Point> BY_X=Comparator.comparingDouble((Point p)->p.x)
        .thenComparingDouble(p->p.y).thenComparingInt(p->p.id);
    static final Comparator<Point> BY_Y=Comparator.comparingDouble((Point p)->p.y)
        .thenComparingDouble(p->p.x).thenComparingInt(p->p.id);
    static final int TRACE_LIMIT=512;
    static volatile double sink;
    static double distance(Point a,Point b){return Math.hypot(a.x-b.x,a.y-b.y);}

    // Each unordered pair is evaluated exactly once: O(N^2) time.
    static Result bruteForce(List<Point> points){
        Result best=new Result(Double.POSITIVE_INFINITY,null,null);
        for(int i=0;i<points.size()-1;i++){
            for(int j=i+1;j<points.size();j++){
                Point a=points.get(i),b=points.get(j);
                double d=distance(a,b);
                if(d<best.distance) best=new Result(d,a,b);
            }
        }
        return best;
    }

    // Initial sorting is included in the timed call. Recursion preserves y-order.
    static Result divideAndConquer(List<Point> points,List<String> events){
        List<Point> px=new ArrayList<>(points),py=new ArrayList<>(points);
        px.sort(BY_X); py.sort(BY_Y);
        return solve(px,py,0,events);
    }
    static Result solve(List<Point> px,List<Point> py,int depth,List<String> events){
        if(px.size()<=3){
            Result best=bruteForce(px);
            record(events,"base",px,depth,null,null,best);
            return best;
        }
        int half=px.size()/2;
        List<Point> left=new ArrayList<>(px.subList(0,half));
        List<Point> right=new ArrayList<>(px.subList(half,px.size()));
        double mid=left.get(left.size()-1).x/2+right.get(0).x/2;
        Set<Integer> leftIds=new HashSet<>();
        for(Point p:left)leftIds.add(p.id);
        List<Point> ly=new ArrayList<>(),ry=new ArrayList<>();
        // Unique IDs are essential when x coordinates or complete points tie.
        for(Point p:py){if(leftIds.contains(p.id))ly.add(p);else ry.add(p);}
        record(events,"split",px,depth,mid,null,null);
        Result a=solve(left,ly,depth+1,events),b=solve(right,ry,depth+1,events);
        Result best=a.distance<=b.distance?a:b;
        double initialD=best.distance;
        record(events,"strip",px,depth,mid,initialD,best);
        if(initialD>0){ // Zero is globally optimal, including duplicate points.
            List<Point> strip=new ArrayList<>();
            for(Point p:py)if(Math.abs(p.x-mid)<initialD)strip.add(p);
            for(int i=0;i<strip.size();i++){
                for(int j=i+1;j<Math.min(i+8,strip.size());j++){
                    Point p=strip.get(i),q=strip.get(j);
                    if(q.y-p.y>=best.distance)break;
                    double d=distance(p,q);
                    if(d<best.distance){
                        best=new Result(d,p,q);
                        record(events,"improve",px,depth,mid,initialD,best);
                    }
                }
            }
        }
        record(events,"merge",px,depth,mid,initialD,best);
        return best;
    }
    static void record(List<String> events,String kind,List<Point> ps,int depth,
                       Double mid,Double d,Result result){
        if(events==null)return;
        StringJoiner ids=new StringJoiner(",","[","]");
        for(Point p:ps)ids.add(Integer.toString(p.id));
        events.add("{\"kind\":"+quote(kind)+",\"ids\":"+ids+",\"depth\":"+depth
            +",\"mid\":"+number(mid)+",\"d\":"+number(d)+",\"pair\":"+pair(result)
            +",\"distance\":"+number(result==null?null:result.distance)+"}");
    }
    static List<Point> generate(int n,long seed,String mode){
        Random random=new Random(seed);
        List<Point> ps=new ArrayList<>();
        for(int i=0;i<n;i++){
            double x=random.nextDouble()*200-100,y=random.nextDouble()*200-100;
            if(mode.equals("same-x"))x=0;
            if(mode.equals("same-y"))y=0;
            if(mode.equals("collinear"))y=.6*x+5;
            if(mode.equals("duplicates")&&i==n-1&&n>=2){x=ps.get(0).x;y=ps.get(0).y;}
            ps.add(new Point(x,y,i));
        }
        return ps;
    }
    static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    static void verify(List<Point> ps,Result a,Result b){
        if(ps.size()<2){
            require(a.a==null&&a.b==null&&b.a==null&&b.b==null
                &&a.distance==Double.POSITIVE_INFINITY&&b.distance==Double.POSITIVE_INFINITY,"Invalid no-pair result");
            return;
        }
        double tolerance=Math.max(1e-12,1e-12*Math.max(Math.abs(a.distance),Math.abs(b.distance)));
        require(Math.abs(a.distance-b.distance)<=tolerance,"Algorithms disagree");
        for(Result r:new Result[]{a,b}){
            require(ps.contains(r.a)&&ps.contains(r.b)&&r.a.id!=r.b.id,"Invalid closest pair");
            require(distance(r.a,r.b)==r.distance,"Pair does not match distance");
        }
    }
    static List<Point> points(double[][] xy){
        List<Point> ps=new ArrayList<>();
        for(int i=0;i<xy.length;i++)ps.add(new Point(xy[i][0],xy[i][1],i));
        return ps;
    }
    static void test(List<Point> ps){verify(ps,bruteForce(ps),divideAndConquer(ps,null));}
    static int runTests(){
        int count=0;
        double[][][] cases={ {},{{1,2}},{{0,0},{3,4}},{{2,2},{2,2}},
            {{0,0},{0,1},{1,0},{1,1}},{{-5,-8},{-4.99,-8.01},{5.5,9.1}},
            {{-100,0},{-1,0},{0,0},{100,0}},{{0,7},{0,3},{0,5},{0,5},{0,9},{0,-2}} };
        for(double[][] xy:cases){test(points(xy));count++;}
        List<Point> identical=new ArrayList<>();
        for(int i=0;i<15;i++)identical.add(new Point(1,1,i));
        test(identical);count++;
        for(String mode:new String[]{"collinear","same-x","same-y"}){test(generate(41,15,mode));count++;}
        for(int mask=0;mask<512;mask++){
            List<Point> ps=new ArrayList<>();
            for(int i=0;i<9;i++)if((mask&(1<<i))!=0)ps.add(new Point(i/3,i%3,ps.size()));
            test(ps);count++;
        }
        Random random=new Random(314159);
        for(int t=0;t<1000;t++){
            int n=random.nextInt(100);
            List<Point> ps=new ArrayList<>();
            for(int i=0;i<n;i++){
                double x=t%2==0?random.nextDouble()*200-100:random.nextInt(17)-8;
                double y=t%2==0?random.nextDouble()*200-100:random.nextInt(17)-8;
                ps.add(new Point(x,y,i));
            }
            test(ps);count++;
        }
        require(bruteForce(points(new double[][]{{0,0},{3,4}})).distance==5,"Known distance test failed");
        return count;
    }
    static Timed timed(List<Point> ps,boolean divide){
        long start=System.nanoTime();
        Result r=divide?divideAndConquer(ps,null):bruteForce(ps);
        double ms=(System.nanoTime()-start)/1e6;
        sink=r.distance;
        return new Timed(r,ms);
    }
    static double median(double[] values){
        Arrays.sort(values);int n=values.length;
        return n%2==1?values[n/2]:(values[n/2-1]+values[n/2])/2;
    }
    static void warmup(){
        List<Point> ps=generate(100,13,"random");
        for(int i=0;i<200;i++){sink=bruteForce(ps).distance;sink=divideAndConquer(ps,null).distance;}
    }
    static String quote(String s){
        return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\"";
    }
    static String number(Double d){return d==null||!Double.isFinite(d)?"null":Double.toString(d);}
    static String pair(Result r){return r==null||r.a==null?"null":"["+r.a.id+","+r.b.id+"]";}
    static String packed(Result r){return "{\"distance\":"+number(r.distance)+",\"pair\":"+pair(r)+"}";}
    static void printResult(String label,Result r){
        if(r.a==null)System.out.println(label+": N/A; no pair (N < 2)");
        else System.out.printf(Locale.US,"%s: %.6f (%s, %s) (%s, %s)%n",label,r.distance,r.a.x,r.a.y,r.b.x,r.b.y);
    }
    public static void main(String[] args)throws Exception{
        Locale.setDefault(Locale.US);
        long seed=42;String mode="random",output="closest_pair_report.html";
        int repeats=5;int[] sizes={100,250,500,1000,2000};
        try{
            for(int i=0;i<args.length;i++){
                if(args[i].equals("--test-only")){System.out.println("PASS: "+runTests()+" correctness cases.");return;}
                if(args[i].equals("--help")){
                    System.out.println("java ClosestPair.java [--seed 42] [--mode random|duplicates|collinear|same-x|same-y] [--sizes 100,500,1000,2000] [--repeats 5] [--output closest_pair_report.html] [--test-only]\nEnter nonnegative N on standard input.");return;
                }
                String key=args[i],value=args[++i];
                switch(key){
                    case "--seed":seed=Long.parseLong(value);break;
                    case "--mode":mode=value;break;
                    case "--output":output=value;break;
                    case "--repeats":repeats=Integer.parseInt(value);break;
                    case "--sizes":sizes=Arrays.stream(value.split(",")).mapToInt(Integer::parseInt).distinct().sorted().toArray();break;
                    default:throw new IllegalArgumentException("Unknown option: "+key);
                }
            }
            if(!Arrays.asList("random","duplicates","collinear","same-x","same-y").contains(mode)||repeats<1||sizes.length==0||Arrays.stream(sizes).anyMatch(n->n<2))throw new IllegalArgumentException("Invalid options");
        }catch(RuntimeException e){System.err.println("Invalid arguments. Run java ClosestPair.java --help");System.exit(1);return;}
        // Prompt goes to stderr to preserve the required three stdout lines.
        System.err.println("Enter number of points N (for example 32), then press Enter:");
        BufferedReader reader=new BufferedReader(new InputStreamReader(System.in));
        int n;
        try{n=Integer.parseInt(reader.readLine().trim());if(n<0)throw new NumberFormatException();}
        catch(Exception e){System.err.println("N must be a nonnegative integer. Example: echo 32 | java ClosestPair.java");System.exit(1);return;}
        List<Point> ps=generate(n,seed,mode);
        warmup();
        Timed a=timed(ps,false),b=timed(ps,true);
        verify(ps,a.result,b.result);
        printResult("Brute-Force",a.result);printResult("Divide-and-Conquer",b.result);
        System.out.printf("Execution time (ms): Brute-Force=%.6f; Divide-and-Conquer=%.6f%n",a.ms,b.ms);
        int tests=runTests();
        System.err.println("PASS: "+tests+" correctness cases.");
        StringJoiner rows=new StringJoiner(",","[","]");
        StringBuilder csv=new StringBuilder("N,brute_force_ms,divide_and_conquer_ms\n");
        for(int size:sizes){
            double[] bt=new double[repeats],dt=new double[repeats];
            for(int r=0;r<repeats;r++){
                List<Point> input=generate(size,seed+size*100L+r,"random");
                Timed ta,tb;
                if(r%2==0){ta=timed(input,false);tb=timed(input,true);}else{tb=timed(input,true);ta=timed(input,false);}
                verify(input,ta.result,tb.result);bt[r]=ta.ms;dt[r]=tb.ms;
            }
            double bm=median(bt),dm=median(dt);
            rows.add("{\"n\":"+size+",\"brute\":"+bm+",\"divide\":"+dm+"}");
            csv.append(size).append(',').append(bm).append(',').append(dm).append('\n');
            System.err.println("Benchmarked N="+size+": answers agree ("+repeats+" trials).");
        }
        List<String> events=new ArrayList<>();
        if(n<=TRACE_LIMIT)divideAndConquer(ps,events);
        else{
            List<Point> xs=new ArrayList<>(ps);xs.sort(BY_X);int h=n/2;
            Result left=divideAndConquer(xs.subList(0,h),null),right=divideAndConquer(xs.subList(h,n),null);
            record(events,"merge",xs,0,xs.get(h-1).x/2+xs.get(h).x/2,Math.min(left.distance,right.distance),b.result);
        }
        StringJoiner jsonPoints=new StringJoiner(",","[","]");
        StringBuilder pcsv=new StringBuilder("x,y,id\n");
        for(Point p:ps){jsonPoints.add("["+p.x+","+p.y+","+p.id+"]");pcsv.append(p.x).append(',').append(p.y).append(',').append(p.id).append('\n');}
        String data="{\"points\":"+jsonPoints+",\"brute\":"+packed(a.result)+",\"divide\":"+packed(b.result)
            +",\"bt\":"+a.ms+",\"dt\":"+b.ms+",\"events\":["+String.join(",",events)+"],\"benchmarks\":"+rows
            +",\"tests\":"+tests+",\"seed\":"+seed+",\"mode\":"+quote(mode)+",\"repeats\":"+repeats
            +",\"traceLimit\":"+TRACE_LIMIT+",\"environment\":"+quote("Java "+System.getProperty("java.version")+" / "+System.getProperty("os.name"))+"}";
        Path target=Paths.get(output).toAbsolutePath();Files.createDirectories(target.getParent());
        Path template=Paths.get("report_template.html");
        if(!Files.exists(template)){System.err.println("Cannot find report_template.html. Run from the folder containing both project files.");System.exit(1);return;}
        Files.writeString(target,Files.readString(template,StandardCharsets.UTF_8).replace("__DATA__",data),StandardCharsets.UTF_8);
        Files.writeString(target.resolveSibling("points.csv"),pcsv.toString(),StandardCharsets.UTF_8);
        Files.writeString(target.resolveSibling("benchmark.csv"),csv.toString(),StandardCharsets.UTF_8);
        System.err.println("Open "+target+" in your browser.");
    }
}
