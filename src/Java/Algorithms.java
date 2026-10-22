import java.util.*;

public final class Algorithms {
    static final class DSU {
        final int[] parent,size;
        int components,largest;
        DSU(int n){
            parent=new int[n];
            size=new int[n];
            components=n;
            largest=n==0?0:1;
            for(int i=0;i<n;i++){parent[i]=i;size[i]=1;}
        }
        int find(int v){
            int root=v;
            while(parent[root]!=root)root=parent[root];
            while(v!=root){int next=parent[v];parent[v]=root;v=next;}
            return root;
        }
        boolean union(int u,int v){
            u=find(u);v=find(v);
            if(u==v)return false;
            if(size[u]<size[v]){int temp=u;u=v;v=temp;}
            parent[v]=u;
            size[u]+=size[v];
            components--;
            largest=Math.max(largest,size[u]);
            return true;
        }
    }
    static Map<String,Object> dsu(Map<String,Object> input){
        DSU sets=new DSU(Input.vertexCount(input));
        int[][] operations=Input.operations(input);
        int[][] states=new int[operations.length][2];
        for(int i=0;i<operations.length;i++){
            sets.union(operations[i][0],operations[i][1]);
            states[i][0]=sets.components;
            states[i][1]=sets.largest;
        }
        return Map.of("states",states);
    }
    static Map<String,Object> kruskal(Map<String,Object> input){
        int n=Input.vertexCount(input);
        int[][] edges=Input.edges(input);
        Integer[] ids=new Integer[edges.length];
        for(int i=0;i<ids.length;i++)ids[i]=i;
        Arrays.sort(ids,Comparator.<Integer>comparingInt(i->edges[i][2]).thenComparingInt(i->i));
        DSU sets=new DSU(n);
        ArrayList<Integer> selected=new ArrayList<>();
        long cost=0;
        for(int id:ids){
            int[] edge=edges[id];
            if(sets.union(edge[0],edge[1])){selected.add(id);cost+=(long)edge[2];}
        }
        return Map.of("cost",cost,"edge_ids",selected,"components",sets.components);
    }
    // Compact adjacency arrays preserve original edge IDs, including parallel edges.
    static final class Graph {
        final int[] head,to,next,id;
        int count;
        Graph(int n,int capacity){
            head=new int[n];Arrays.fill(head,-1);
            to=new int[capacity];next=new int[capacity];id=new int[capacity];
        }
        void add(int from,int target,int edgeId){
            to[count]=target;id[count]=edgeId;next[count]=head[from];head[from]=count++;
        }
    }
    static Graph directed(int n,int[][] edges,boolean transpose){
        Graph graph=new Graph(n,edges.length);
        for(int i=0;i<edges.length;i++){
            graph.add(edges[i][transpose?1:0],edges[i][transpose?0:1],i);
        }
        return graph;
    }
    static Map<String,Object> prim(Map<String,Object> input){
        int n=Input.vertexCount(input);
        int[][] edges=Input.edges(input);
        Graph graph=new Graph(n,2*edges.length);
        for(int i=0;i<edges.length;i++){
            graph.add(edges[i][0],edges[i][1],i);
            graph.add(edges[i][1],edges[i][0],i);
        }
        boolean[] visited=new boolean[n];
        PriorityQueue<Integer> heap=new PriorityQueue<>(
            Comparator.<Integer>comparingInt(i->edges[graph.id[i]][2]).thenComparingInt(i->graph.id[i]));
        ArrayList<Integer> selected=new ArrayList<>();
        int components=0;
        long cost=0;
        for(int start=0;start<n;start++){
            if(visited[start])continue;
            components++;
            visited[start]=true;
            for(int e=graph.head[start];e!=-1;e=graph.next[e]){
                if(!visited[graph.to[e]])heap.add(e);
            }
            while(!heap.isEmpty()){
                int arc=heap.remove(),v=graph.to[arc];
                if(visited[v])continue;
                visited[v]=true;
                int id=graph.id[arc];
                selected.add(id);cost+=(long)edges[id][2];
                for(int e=graph.head[v];e!=-1;e=graph.next[e]){
                    if(!visited[graph.to[e]])heap.add(e);
                }
            }
        }
        return Map.of("cost",cost,"edge_ids",selected,"components",components);
    }
    // Each stack frame keeps its next arc, so an adjacency list is scanned once.
    // A null return means a back edge was found when cycle detection was requested.
    static int[] finishOrder(Graph graph,boolean rejectCycle){
        int n=graph.head.length,used=0;
        byte[] state=new byte[n];
        int[] stack=new int[n],cursor=new int[n],finish=new int[n];
        for(int start=0;start<n;start++){
            if(state[start]!=0)continue;
            int top=0;
            stack[top]=start;cursor[top]=graph.head[start];state[start]=1;
            while(top>=0){
                int v=stack[top],arc=cursor[top];
                if(arc==-1){state[v]=2;finish[used++]=v;top--;continue;}
                cursor[top]=graph.next[arc];
                int w=graph.to[arc];
                if(state[w]==1&&rejectCycle)return null;
                if(state[w]==0){
                    state[w]=1;stack[++top]=w;cursor[top]=graph.head[w];
                }
            }
        }
        return finish;
    }
    static Map<String,Object> scc(Map<String,Object> input){
        int n=Input.vertexCount(input);
        int[][] edges=Input.edges(input);
        Graph graph=directed(n,edges,false),transpose=directed(n,edges,true);
        int[] finish=finishOrder(graph,false),component=new int[n],stack=new int[n];
        Arrays.fill(component,-1);
        int label=0;
        for(int i=n-1;i>=0;i--){
            int start=finish[i];
            if(component[start]!=-1)continue;
            int top=0;stack[top]=start;component[start]=label;
            while(top>=0){
                int v=stack[top--];
                for(int e=transpose.head[v];e!=-1;e=transpose.next[e]){
                    int w=transpose.to[e];
                    if(component[w]==-1){component[w]=label;stack[++top]=w;}
                }
            }
            label++;
        }
        HashSet<Long> seen=new HashSet<>();
        ArrayList<int[]> condensation=new ArrayList<>();
        for(int[] edge:edges){
            int a=component[edge[0]],b=component[edge[1]];
            long key=((long)a<<32)|(b&0xffffffffL);
            if(a!=b&&seen.add(key))condensation.add(new int[]{a,b});
        }
        return Map.of("component",component,"condensation",condensation);
    }
    static Map<String,Object> topo(Map<String,Object> input){
        int n=Input.vertexCount(input);
        int[] finish=finishOrder(directed(n,Input.edges(input),false),true);
        if(finish==null)return Map.of("acyclic",false,"order",new int[0]);
        int[] order=new int[n];
        for(int i=0;i<n;i++)order[i]=finish[n-1-i];
        return Map.of("acyclic",true,"order",order);
    }
    static Map<String,Object> dag(Map<String,Object> input){
        int n=Input.vertexCount(input),source=Input.source(input);
        int[][] edges=Input.edges(input);
        Graph graph=directed(n,edges,false);
        int[] finish=finishOrder(graph,true);
        if(finish==null)return Map.of("acyclic",false,"distance",new Long[0],"parent_edge",new int[0]);
        Long[] distance=new Long[n];
        int[] parent=new int[n];Arrays.fill(parent,-1);
        distance[source]=0L;
        for(int i=n-1;i>=0;i--){
            int u=finish[i];
            if(distance[u]==null)continue;
            for(int e=graph.head[u];e!=-1;e=graph.next[e]){
                int v=graph.to[e],id=graph.id[e];
                long candidate=distance[u]+(long)edges[id][2];
                if(distance[v]==null||candidate<distance[v]){
                    distance[v]=candidate;parent[v]=id;
                }
            }
        }
        return Map.of("acyclic",true,"distance",distance,"parent_edge",parent);
    }
    // Precondition: distance and parentEdge are the acyclic result for these edges/source.
    static List<Integer> reconstructRoute(int source,int target,int[][] edges,Long[] distance,int[] parentEdge){
        ArrayList<Integer> route=new ArrayList<>();
        if(distance[target]==null)return route;
        for(int v=target;;v=edges[parentEdge[v]][0]){
            route.add(v);
            if(v==source)break;
        }
        Collections.reverse(route);
        return route;
    }
}
