from pathlib import Path
import hashlib,json,os,subprocess,time
root=Path('/private/tmp/mvpa4s-eco-u6-public-consumer-20261022');ivy=Path('/private/tmp/mvpa4s-eco-u6-public-ivy-20261022/local')
def inventory(base):
 return {str(p.relative_to(base)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(base.rglob('*')) if p.is_file() and not any(x in p.relative_to(base).parts for x in ['target','.git','.jvm','.js','.bsp','.bloop'])}
before=inventory(root);artifacts=inventory(ivy)
boot={}
for name,revision in [('gale','779ed3d8169cdd9f4eb9ab10b1ca9defadc27715'),('resample','6bc4172a966c92f1b06811eac64ac2bada9fef9b')]:
 p=Path('/private/tmp/mvpa4s-eco-u6-'+name+'-bootstrap-20261022')
 assert subprocess.check_output(['git','-C',str(p),'rev-parse','HEAD'],text=True).strip()==revision
 assert not subprocess.check_output(['git','-C',str(p),'status','--porcelain','--untracked-files=no'],text=True).strip()
 boot[name]={'revision':revision,'clean_tracked_source':True}
argv=['sbt','-Dsbt.global.base=/private/tmp/mvpa4s-eco-u6-public-sbt-20261022','-Dsbt.ivy.home=/private/tmp/mvpa4s-eco-u6-public-ivy-20261022','-Dsbt.coursier.home=/private/tmp/mvpa4s-eco-u6-public-coursier-20261022','surfaceJVM/test','atlasJVM/test','surfaceJS/test','atlasJS/test','show surfaceJVM/Compile/fullClasspath','show atlasJVM/Compile/fullClasspath','show atlasJS/Compile/fullClasspath']
start=time.monotonic();result=subprocess.run(argv,cwd=root,check=False)
assert inventory(root)==before,'consumer source changed'
assert inventory(ivy)==artifacts,'private published artifact changed'
expected={'mvpa4s':'c27469484bf760c34cd896dabe4fe1c384a83b45','gale':'779ed3d8169cdd9f4eb9ab10b1ca9defadc27715','resample4s':'6bc4172a966c92f1b06811eac64ac2bada9fef9b','locus4s':'e32b0aeba5c4d19c457b2329272b95b26c061943','multivar':'7afdeea290fdcc80b81a4126aa303f93a5348c0e','neuroatlas4s':'4df875d8a1cd20afd3b83ed49446422bc507e5ec','neurosurf4s':'73a244fbe5d7b4db2f0c946d98fa2a3c00101ab7'}
providers={}
for gitdir in Path('/private/tmp/mvpa4s-eco-u6-public-sbt-20261022/staging').glob('*/*/.git'):
 p=gitdir.parent
 if p.name not in expected:continue
 revision=subprocess.check_output(['git','-C',str(p),'rev-parse','HEAD'],text=True).strip();assert revision==expected[p.name],p
 assert not subprocess.check_output(['git','-C',str(p),'status','--porcelain','--untracked-files=no'],text=True).strip(),p
 assert p.name not in providers,p
 providers[p.name]={'revision':revision,'path':str(p),'clean_tracked_source':True}
assert set(providers)==set(expected)
receipt={'schema':'mvpa4s.eco-independent-consumer.v1','source_commit':expected['mvpa4s'],'consumer_inputs':before,'private_artifact_inventory':artifacts,'bootstrap_sources':boot,'providers':providers,'argv':argv,'exit_code':result.returncode,'elapsed_seconds':time.monotonic()-start,'consumer_and_artifacts_unchanged_after':True,'limits':'Separate public Git-pinned source consumer, no source overrides or provider test dependencies. Exact Gale/Resample source-published private artifacts; not Maven Central publication, main landing, native or new scientific qualification.'}
p=Path('/tmp/mvpa4s-eco-u6-independent-06.json');assert not p.exists();p.write_text(json.dumps(receipt,indent=2)+'\n');print('receipt',p,flush=True)
raise SystemExit(result.returncode)
