import java.util.*;

public class GraphChecks {
    static Map<String,Object> request(int n,int[][] edges){
        List<List<Integer>> rows=new ArrayList<>();
        for(int[] edge:edges){List<Integer> row=new ArrayList<>();for(int x:edge)row.add(x);rows.add(row);}
        return Map.of("n",n,"edges",rows);
    }
    static void require(boolean value){if(!value)throw new AssertionError();}
    static void routes(){
        int[][] edges={{0,1,3},{0,2,8},{1,2,-7},{2,3,4},{0,1,3}};
        Map<String,Object> input=new HashMap<>(request(5,edges));input.put("source",0);
        Map<String,Object> result=Algorithms.dag(input);
        Long[] distance=(Long[])result.get("distance");
        int[] parents=(int[])result.get("parent_edge");
        List<List<Integer>> expected=List.of(List.of(0,1,2,3),List.of(0),List.of(),List.of(0,1));
        int[] targets={3,0,4,1};
        for(int i=0;i<targets.length;i++){
            int target=targets[i];
            List<Integer> route=Algorithms.reconstructRoute(0,target,edges,distance,parents);
            require(route.equals(expected.get(i)));
            List<Integer> ids=new ArrayList<>();long cost=0;
            for(int j=1;j<route.size();j++){
                int id=parents[route.get(j)];
                require(edges[id][0]==route.get(j-1)&&edges[id][1]==route.get(j));
                ids.add(id);cost+=edges[id][2];
            }
            if(!route.isEmpty())require(cost==distance[target]);
            System.out.println("target="+target+" route="+route+" edge_ids="+ids+" cost="+(route.isEmpty()?"unreachable":cost));
        }
        System.out.println("parent_edge="+Arrays.toString(parents));
    }
    static void forest(int n,int[][] edges,Map<String,Object> answer){
        Algorithms.DSU sets=new Algorithms.DSU(n);
        long sum=0;
        @SuppressWarnings("unchecked") List<Integer> ids=(List<Integer>)answer.get("edge_ids");
        HashSet<Integer> seen=new HashSet<>();
        for(int id:ids){require(seen.add(id));int[] e=edges[id];require(sets.union(e[0],e[1]));sum+=e[2];}
        require(sum==((Number)answer.get("cost")).longValue());
        require(sets.components==((Number)answer.get("components")).intValue());
        for(int[] edge:edges)require(sets.find(edge[0])==sets.find(edge[1]));
    }
    static void benchmark(){
        System.out.println("runtime="+System.getProperty("java.runtime.version"));
        System.out.println("os="+System.getProperty("os.name")+" "+System.getProperty("os.version")+" "+System.getProperty("os.arch"));
        System.out.println("processors="+Runtime.getRuntime().availableProcessors());
        System.out.println("timer=System.nanoTime; milliseconds; seed=2515+n");
        for(int n:new int[]{1000,10000,100000}){
            int m=Math.min(250000,4*n);
            int[][] edges=new int[m][3];Random random=new Random(2515L+n);
            for(int i=0;i<m;i++){
                edges[i][0]=i<n-1?i:random.nextInt(n);
                edges[i][1]=i<n-1?i+1:random.nextInt(n);
                edges[i][2]=random.nextInt(2000000001)-1000000000;
            }
            Map<String,Object> input=request(n,edges);
            Map<String,Object> k=Algorithms.kruskal(input),p=Algorithms.prim(input);
            forest(n,edges,k);forest(n,edges,p);require(k.get("cost").equals(p.get("cost")));
            for(int warm=0;warm<3;warm++){Algorithms.kruskal(input);Algorithms.prim(input);}
            double[][] times=new double[2][3];
            for(int round=0;round<3;round++){
                for(int turn=0;turn<2;turn++){
                    int method=(round+turn)%2;
                    long start=System.nanoTime();
                    Map<String,Object> answer=method==0?Algorithms.kruskal(input):Algorithms.prim(input);
                    times[method][round]=(System.nanoTime()-start)/1e6;
                    forest(n,edges,answer);require(answer.get("cost").equals(k.get("cost")));
                }
            }
            for(int method=0;method<2;method++){
                double[] sorted=times[method].clone();Arrays.sort(sorted);
                System.out.printf(Locale.ROOT,"n=%d m=%d %s samples_ms=%s median_ms=%.6f cost=%s%n",n,m,method==0?"kruskal":"prim",Arrays.toString(times[method]),sorted[1],k.get("cost"));
            }
        }
    }
    public static void main(String[] args){
        if(args.length>0&&args[0].equals("benchmark"))benchmark();else routes();
    }
}
