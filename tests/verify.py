"""Independent small-graph oracles, full-limit cases and semantic witness tests.
Run from repository root after compiling: python tests/verify.py
Set JAVA to an absolute java executable if java is not on PATH.
"""
import copy,itertools,json,os,random,subprocess,sys,time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from public_validate import check
OUT=ROOT/'tests'
SEED=2515

def oracle(x):
 task=x['task'];n=x.get('n',0);edges=x.get('edges',[])
 if task=='ping':return {'status':'ready'}
 if task=='dsu':
  groups=[{i} for i in range(n)];states=[]
  for u,v in x['operations']:
   a=next(g for g in groups if u in g);b=next(g for g in groups if v in g)
   if a is not b:a.update(b);groups.remove(b)
   states.append([len(groups),max(map(len,groups),default=0)])
  return {'states':states}
 reach=[[i==j for j in range(n)] for i in range(n)]
 for u,v,*_ in edges:
  reach[u][v]=True
  if task in ('kruskal','prim'):reach[v][u]=True
 for k in range(n):
  for i in range(n):
   for j in range(n):reach[i][j]|=reach[i][k] and reach[k][j]
 if task in ('kruskal','prim'):
  components=sum(not any(reach[i][j] for j in range(i)) for i in range(n))
  best=None
  for ids in itertools.combinations(range(len(edges)),n-components):
   groups=[{i} for i in range(n)];cost=0
   for eid in ids:
    u,v,w=edges[eid];a=next(g for g in groups if u in g);b=next(g for g in groups if v in g)
    if a is b:break
    a.update(b);groups.remove(b);cost+=w
   else:
    if len(groups)==components and (best is None or cost<best):best=cost
  return {'cost':best,'components':components}
 if task=='scc':
  labels=[next(j for j in range(n) if reach[i][j] and reach[j][i]) for i in range(n)]
  return {'component':labels}
 cyclic=any(u==v or reach[v][u] for u,v,*_ in edges)
 if task=='topo':return {'acyclic':not cyclic}
 if cyclic:return {'acyclic':False}
 # Bellman-Ford, independent of DFS ordering.
 dist=[None]*n;dist[x['source']]=0
 for _ in range(n-1):
  changed=False
  for u,v,w in edges:
   if dist[u] is not None and (dist[v] is None or dist[u]+w<dist[v]):dist[v]=dist[u]+w;changed=True
  if not changed:break
 return {'acyclic':True,'distance':dist}

def execute(requests,filename):
 data=''.join(json.dumps(x,separators=(',',':'))+'\n' for x in requests)
 (OUT/(filename+'_input.jsonl')).write_text(data,encoding='utf-8')
 start=time.perf_counter()
 process=subprocess.run([os.environ.get('JAVA','java'),'-Xmx384m','-cp',str(ROOT/'.build/Java'),'Main'],input=data,encoding='utf-8',capture_output=True,timeout=20)
 elapsed=time.perf_counter()-start
 assert process.returncode==0,process.stderr
 assert not process.stderr,process.stderr
 (OUT/(filename+'_actual.jsonl')).write_text(process.stdout,encoding='utf-8')
 results=[json.loads(line) for line in process.stdout.splitlines()]
 assert len(results)==len(requests)
 return results,elapsed

def small_cases():
 cases=[{'task':'ping'}]
 for task in ['dsu','kruskal','prim','scc','topo']:
  cases.append({'task':task,'n':0,'operations' if task=='dsu' else 'edges':[]})
 cases.extend([
 {'task':'dsu','n':5,'operations':[[0,0],[0,1],[1,0],[3,4],[1,3],[2,4]]},
 {'task':'dag','n':5,'source':0,'edges':[[0,1,3],[0,2,8],[1,2,-7],[2,3,4],[0,1,3]]},
 {'task':'dag','n':4,'source':0,'edges':[[0,1,1],[2,3,-5],[3,2,-5]]},
 {'task':'dag','n':4,'source':0,'edges':[[0,1,1000000000],[1,2,1000000000],[2,3,1000000000]]}])
 for task in ['kruskal','prim']:
  cases.append({'task':task,'n':6,'edges':[[0,0,-9],[0,1,2],[0,1,-1],[1,2,-2],[0,2,7],[3,4,1000000000]]})
 rng=random.Random(SEED)
 for _ in range(75):
  n=rng.randrange(1,7);m=rng.randrange(0,11)
  weighted=[[rng.randrange(n),rng.randrange(n),rng.randrange(-9,10)] for _ in range(m)]
  for task in ['kruskal','prim','scc','topo','dag','dsu']:
   if task=='dsu':case={'task':task,'n':n,'operations':[e[:2] for e in weighted]}
   else:case={'task':task,'n':n,'edges':weighted if task in ['kruskal','prim','dag'] else [e[:2] for e in weighted]}
   if task=='dag':
    case['source']=rng.randrange(n)
    # Half of DAG cases are acyclic by construction, with parallel/tied edges.
    if _%2==0:case['edges']=[[min(u,v),max(u,v),w] for u,v,w in weighted if u!=v]
   cases.append(case)
 return cases

def witnesses():
 outcomes=[]
 samples=[
 ({'task':'kruskal','n':3,'edges':[[0,1,1],[1,2,1],[0,2,1]]},{'cost':2,'components':1},{'cost':2,'components':1,'edge_ids':[0,2]},{'cost':2,'components':1,'edge_ids':[0,0]}),
 ({'task':'prim','n':3,'edges':[[0,1,1],[1,2,1],[0,2,1]]},{'cost':2,'components':1},{'cost':2,'components':1,'edge_ids':[1,2]},{'cost':2,'components':1,'edge_ids':[1,1]}),
 ({'task':'scc','n':3,'edges':[[0,1],[1,0],[1,2]]},{'component':[0,0,1]},{'component':[7,7,9],'condensation':[[7,9]]},{'component':[7,7,9],'condensation':[]}),
 ({'task':'topo','n':3,'edges':[[0,2],[1,2]]},{'acyclic':True},{'acyclic':True,'order':[1,0,2]},{'acyclic':True,'order':[2,1,0]}),
 ({'task':'dag','n':3,'source':0,'edges':[[0,1,1],[0,1,1],[1,2,2]]},{'acyclic':True,'distance':[0,1,3]},{'acyclic':True,'distance':[0,1,3],'parent_edge':[-1,1,2]},{'acyclic':True,'distance':[0,1,3],'parent_edge':[-1,2,1]})]
 for x,e,good,bad in samples:
  assert check(x,good,e) and not check(x,bad,e)
  outcomes.append({'input':x,'expected':e,'alternative_valid':good,'invalid_witness':bad,'accepted_valid':True,'rejected_invalid':True})
 (OUT/'witness_checks.json').write_text(json.dumps(outcomes,indent=2),encoding='utf-8')
 return len(samples)

def scale_cases():
 n=100000;m=250000
 operations=[[i,i+1] for i in range(n-1)]
 operations.extend([[0,n-1]]*(m-len(operations)))
 weighted=[[i,i+1,-1000000000] for i in range(n-1)]
 weighted.extend([[0,n-1,1000000000]]*(m-len(weighted)))
 directed=[e[:2] for e in weighted]
 cases=[{'task':'dsu','n':n,'operations':operations}]
 expected=[{'states':[[n-i-1,i+2] for i in range(n-1)]+[[1,n]]*(m-n+1)}]
 for task in ['kruskal','prim']:
  cases.append({'task':task,'n':n,'edges':weighted});expected.append({'cost':-(n-1)*1000000000,'components':1})
 cases.append({'task':'scc','n':n,'edges':directed});expected.append({'component':list(range(n))})
 cases.append({'task':'topo','n':n,'edges':directed});expected.append({'acyclic':True})
 cases.append({'task':'dag','n':n,'edges':weighted,'source':0});expected.append({'acyclic':True,'distance':[-i*1000000000 for i in range(n)]})
 # A 100,000-vertex cycle tests SCC second pass and cycle detection at depth.
 cycle=[[i,i+1] for i in range(n-1)]+[[n-1,0]]
 cases.append({'task':'scc','n':n,'edges':cycle});expected.append({'component':[0]*n})
 cases.append({'task':'topo','n':n,'edges':cycle});expected.append({'acyclic':False})
 cases.append({'task':'dag','n':n,'source':0,'edges':[[u,v,1] for u,v in cycle]});expected.append({'acyclic':False})
 return cases,expected

def main():
 lines=[]
 cases=small_cases();expected=[oracle(x) for x in cases]
 # Repeat mixed requests inside a single process to expose leaked state.
 cases=cases+cases;expected=expected+expected
 answers,elapsed=execute(cases,'small')
 for i,(x,a,e) in enumerate(zip(cases,answers,expected)):assert check(x,a,e),(i,x,a,e)
 (OUT/'small_expected.jsonl').write_text(''.join(json.dumps(e)+'\n' for e in expected),encoding='utf-8')
 lines.append(f'PASS {len(cases)} small/mixed requests against independent oracles ({elapsed:.3f}s process time)')
 count=witnesses();lines.append(f'PASS {count} alternative valid answers and {count} invalid witnesses')
 cases,expected=scale_cases();answers,elapsed=execute(cases,'scale')
 for i,(x,a,e) in enumerate(zip(cases,answers,expected)):assert check(x,a,e),('scale',i)
 (OUT/'scale_expected.jsonl').write_text(''.join(json.dumps(e,separators=(',',':'))+'\n' for e in expected),encoding='utf-8')
 lines.append(f'PASS {len(cases)} full/deep cases; n=100000, up to 250000 rows ({elapsed:.3f}s process time)')
 (OUT/'own_results.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8')
 print('\n'.join(lines))
if __name__=='__main__':main()
