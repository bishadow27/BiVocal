import { useEffect,useMemo,useState,type FormEvent } from 'react';
import { keepPreviousData,useMutation,useQuery,useQueryClient } from '@tanstack/react-query';
import { CheckCircle2,Pencil,Search,Upload,XCircle,Undo2 } from 'lucide-react';
import { adminApi } from '../api/admin.api';
import type { Level,ReviewStatus,VocabularyPublishPreview,VocabularyReviewCandidate,VocabularyReviewEdit,VocabularyReviewFilters } from '../types/api';
import { Badge,Button,Dialog,ErrorPanel,PageHeader,Pagination,Panel,QueryView,SelectField,TextField,Truncate,useToast } from '../components/ui';
import { useListState } from '../hooks/useListState';
import { errorMessage } from '../api/errors';

const levels=['A1','A2','B1','B2'];
const statusLabel:Record<ReviewStatus,string>={PENDING:'Chờ duyệt',APPROVED:'Đã duyệt',REJECTED:'Từ chối',PUBLISHED:'Đã xuất bản'};
const tone=(s:ReviewStatus)=>s==='APPROVED'||s==='PUBLISHED'?'green':s==='REJECTED'?'red':s==='PENDING'?'blue':'neutral';

function CandidateEditor({candidate,onClose,onSaved}:{candidate:VocabularyReviewCandidate;onClose:()=>void;onSaved:()=>void}){
  const [form,setForm]=useState({english:candidate.english,vietnamese:candidate.vietnamese,pronunciation:candidate.pronunciation??'',partOfSpeech:candidate.partOfSpeech??'',cefr:candidate.cefr??'',primaryTopicCode:candidate.primaryTopicCode??'',topicCodes:candidate.topicCodes.join(', '),example:candidate.example??'',exampleVi:candidate.exampleVi??''});
  const mutation=useMutation({mutationFn:(body:VocabularyReviewEdit)=>adminApi.updateReviewCandidate(candidate.candidateId,body),onSuccess:()=>{onSaved();onClose();}});
  const set=(key:keyof typeof form,value:string)=>setForm(old=>({...old,[key]:value}));
  function submit(e:FormEvent){e.preventDefault();const topics=[...new Set(form.topicCodes.split(',').map(v=>v.trim().toUpperCase()).filter(Boolean))];
    mutation.mutate({english:form.english.trim(),vietnamese:form.vietnamese.trim(),pronunciation:form.pronunciation.trim()||null,partOfSpeech:form.partOfSpeech.trim()||null,cefr:form.cefr as Level,primaryTopicCode:form.primaryTopicCode.trim().toUpperCase(),topicCodes:topics,example:form.example.trim(),exampleVi:form.exampleVi.trim()||null,rowVersion:candidate.rowVersion});}
  return <Dialog title={`Kiểm duyệt · ${candidate.english}`} onClose={onClose} busy={mutation.isPending} drawer><form onSubmit={submit}>
    <div className="dialog-body"><p className="form-intro">Mọi chỉnh sửa được ghi audit và chuyển bản ghi về Chờ duyệt.</p>
      <TextField label="English" value={form.english} onChange={v=>set('english',v)} required maxLength={150}/>
      <TextField label="Vietnamese" value={form.vietnamese} onChange={v=>set('vietnamese',v)} required maxLength={255}/>
      <div className="form-grid"><TextField label="IPA" value={form.pronunciation} onChange={v=>set('pronunciation',v)} maxLength={150}/><TextField label="Từ loại" value={form.partOfSpeech} onChange={v=>set('partOfSpeech',v)} maxLength={30}/></div>
      <div className="form-grid"><SelectField label="CEFR" value={form.cefr} onChange={v=>set('cefr',v)} required options={[{value:'',label:'Chọn CEFR'},...levels.map(v=>({value:v,label:v}))]}/><TextField label="Chủ đề chính" value={form.primaryTopicCode} onChange={v=>set('primaryTopicCode',v)} required maxLength={50}/></div>
      <TextField label="Các chủ đề (phân cách bằng dấu phẩy)" value={form.topicCodes} onChange={v=>set('topicCodes',v)} required maxLength={300}/>
      <TextField label="Ví dụ tiếng Anh" value={form.example} onChange={v=>set('example',v)} required multiline maxLength={5000}/><TextField label="Ví dụ tiếng Việt" value={form.exampleVi} onChange={v=>set('exampleVi',v)} multiline maxLength={5000}/>
      <div className="review-source"><strong>Nguồn</strong><span>{candidate.source.name} · {candidate.source.license}</span><span>{candidate.source.recordId}</span></div>
      {!!candidate.validationReasons.length&&<div className="review-warnings"><strong>Cần kiểm tra</strong>{candidate.validationReasons.map(v=><span key={v}>{v}</span>)}</div>}
      {mutation.isError&&<p className="form-error" role="alert">{errorMessage(mutation.error)}</p>}
    </div><footer className="dialog-footer"><Button onClick={onClose}>Hủy</Button><Button type="submit" variant="primary" busy={mutation.isPending}>Lưu và chuyển về chờ duyệt</Button></footer>
  </form></Dialog>;
}

function PublishDialog({preview,onClose,onPublished}:{preview:VocabularyPublishPreview;onClose:()=>void;onPublished:()=>void}){
  const mutation=useMutation({mutationFn:()=>adminApi.confirmPublish(preview.batchId),onSuccess:onPublished});
  return <Dialog title="Xác nhận lô xuất bản" onClose={onClose} busy={mutation.isPending}>
    <div className="dialog-body"><p className="form-intro">Preview này chỉ áp dụng cho các phiên bản bản ghi hiện tại. Backend sẽ kiểm tra lại trong transaction.</p>
      <div className="publish-preview-grid"><div><span>Sẽ thêm</span><strong>{preview.toInsert}</strong></div><div><span>Bỏ qua trùng</span><strong>{preview.toSkip}</strong></div><div><span>Bị chặn</span><strong>{preview.blocked}</strong></div></div>
      {preview.blocked>0&&<p className="form-error">Lô có bản ghi bị chặn. Hãy đóng preview và sửa lựa chọn.</p>}
      {mutation.isError&&<p className="form-error" role="alert">{errorMessage(mutation.error)}</p>}
    </div><footer className="dialog-footer"><Button onClick={onClose}>Đóng</Button><Button variant="primary" busy={mutation.isPending} disabled={preview.blocked>0} onClick={()=>mutation.mutate()}><Upload size={16}/>Duyệt &amp; xuất bản</Button></footer>
  </Dialog>;
}

export function VocabularyReviewPage(){
  const list=useListState(),client=useQueryClient(),toast=useToast();
  const [filters,setFilters]=useState({level:'',topicCode:'',partOfSpeech:'',status:'PENDING',reason:'',source:''});
  const [selected,setSelected]=useState<Set<string>>(new Set()),[editor,setEditor]=useState<VocabularyReviewCandidate|null>(null),[preview,setPreview]=useState<VocabularyPublishPreview|null>(null);
  const queryFilters:VocabularyReviewFilters={...list.filters,...filters};
  const stats=useQuery({queryKey:['review-stats'],queryFn:({signal})=>adminApi.reviewStats(signal)}),options=useQuery({queryKey:['review-options'],queryFn:({signal})=>adminApi.reviewOptions(signal)});
  const query=useQuery({queryKey:['review-candidates',queryFilters],queryFn:({signal})=>adminApi.reviewCandidates(queryFilters,signal),placeholderData:keepPreviousData});
  useEffect(()=>setSelected(new Set()),[list.page,list.limit,list.committed,filters]);
  const refresh=()=>{void client.invalidateQueries({queryKey:['review-candidates']});void client.invalidateQueries({queryKey:['review-stats']});};
  const bulk=useMutation({mutationFn:({status}:{status:'PENDING'|'APPROVED'|'REJECTED'})=>adminApi.bulkReviewStatus([...selected],status),onSuccess:(r)=>{toast(`Đã cập nhật ${r.changed} ứng viên.`);setSelected(new Set());refresh();}});
  const previewMutation=useMutation({mutationFn:()=>adminApi.previewPublish([...selected]),onSuccess:setPreview});
  const pageIds=useMemo(()=>query.data?.items.map(i=>i.candidateId)??[],[query.data]);const allPage=pageIds.length>0&&pageIds.every(id=>selected.has(id));
  const choose=(id:string,checked:boolean)=>setSelected(old=>{const n=new Set(old);checked?n.add(id):n.delete(id);return n;});
  const setFilter=(key:keyof typeof filters,value:string)=>{setFilters(old=>({...old,[key]:value}));list.setPage(1);};
  return <><PageHeader title="Kiểm duyệt Dataset" description="Kiểm tra, duyệt và xuất bản từ vựng có audit log"/>
    <div className="review-kpis">{stats.data&&([['Tổng ứng viên',stats.data.total],['Chờ duyệt',stats.data.pending],['Đã duyệt',stats.data.approved],['Từ chối',stats.data.rejected],['Đã xuất bản',stats.data.published]] as const).map(([label,value])=><div className="panel" key={label}><span>{label}</span><strong>{value.toLocaleString('vi-VN')}</strong></div>)}</div>
    {stats.isError&&<ErrorPanel error={stats.error}/>}<Panel><div className="toolbar filters-toolbar review-toolbar"><div className="search-field"><Search size={17}/><input aria-label="Tìm ứng viên" placeholder="English hoặc Vietnamese…" value={list.search} onChange={e=>list.setSearch(e.target.value)} maxLength={200}/></div>
      <SelectField label="CEFR" value={filters.level} onChange={v=>setFilter('level',v)} options={[{value:'',label:'Tất cả'},...levels.map(v=>({value:v,label:v}))]}/>
      <SelectField label="Chủ đề" value={filters.topicCode} onChange={v=>setFilter('topicCode',v)} options={[{value:'',label:'Tất cả'},...(options.data?.topics??[]).map(v=>({value:v,label:v}))]}/>
      <SelectField label="Từ loại" value={filters.partOfSpeech} onChange={v=>setFilter('partOfSpeech',v)} options={[{value:'',label:'Tất cả'},...(options.data?.parts??[]).map(v=>({value:v,label:v}))]}/>
      <SelectField label="Trạng thái" value={filters.status} onChange={v=>setFilter('status',v)} options={[{value:'',label:'Tất cả'},...Object.entries(statusLabel).map(([value,label])=>({value,label}))]}/>
      <SelectField label="Lý do" value={filters.reason} onChange={v=>setFilter('reason',v)} options={[{value:'',label:'Tất cả'},...(options.data?.reasons??[]).map(v=>({value:v,label:v}))]}/>
      <SelectField label="Nguồn" value={filters.source} onChange={v=>setFilter('source',v)} options={[{value:'',label:'Tất cả'},...(options.data?.sources??[]).map(v=>({value:v.value,label:v.label}))]}/></div>
      <div className="review-bulk"><label className="checkbox-field"><input type="checkbox" checked={allPage} onChange={e=>pageIds.forEach(id=>choose(id,e.target.checked))}/>Chọn tất cả trên trang</label><span>{selected.size} mục đã chọn</span>
        <Button disabled={!selected.size||bulk.isPending} onClick={()=>bulk.mutate({status:'APPROVED'})}><CheckCircle2 size={15}/>Duyệt</Button><Button disabled={!selected.size||bulk.isPending} onClick={()=>bulk.mutate({status:'REJECTED'})}><XCircle size={15}/>Từ chối</Button><Button disabled={!selected.size||bulk.isPending} onClick={()=>bulk.mutate({status:'PENDING'})}><Undo2 size={15}/>Chờ duyệt</Button><Button variant="primary" disabled={!selected.size||previewMutation.isPending} busy={previewMutation.isPending} onClick={()=>previewMutation.mutate()}><Upload size={15}/>Xem trước xuất bản</Button></div>
      {(bulk.isError||previewMutation.isError)&&<p className="form-error review-error">{errorMessage(bulk.error??previewMutation.error)}</p>}
      <QueryView query={query}>{data=><><div className="table-scroll"><table><thead><tr><th>Chọn</th><th>English / Vietnamese</th><th>CEFR</th><th>Topic</th><th>Cảnh báo</th><th>Nguồn</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>{data.items.map(c=><tr key={c.candidateId}><td><input type="checkbox" aria-label={`Chọn ${c.english}`} checked={selected.has(c.candidateId)} onChange={e=>choose(c.candidateId,e.target.checked)}/></td><td><strong><Truncate value={c.english}/></strong><br/><span className="muted"><Truncate value={c.vietnamese}/></span><br/><small>{c.pronunciation||'—'} · {c.partOfSpeech||'—'}</small></td><td><Badge>{c.cefr||'—'}</Badge></td><td><Badge tone="blue">{c.primaryTopicCode||'Chưa gán'}</Badge></td><td>{c.duplicate.type!=='NONE'&&<Badge tone="red">{c.duplicate.type}</Badge>} {c.multiSense&&<Badge tone="blue">Nhiều nghĩa</Badge>}<div className="review-reasons">{c.validationReasons.slice(0,2).join(' · ')}</div></td><td><span title={c.source.url}>{c.source.name}</span><br/><small>{c.source.license}</small></td><td><Badge tone={tone(c.status)}>{statusLabel[c.status]}</Badge>{c.reviewer&&<small className="reviewer">{c.reviewer}</small>}</td><td><Button variant="quiet" disabled={c.status==='PUBLISHED'} onClick={()=>setEditor(c)}><Pencil size={14}/>Sửa</Button></td></tr>)}</tbody></table></div><Pagination {...data} busy={query.isFetching} onPage={list.setPage} onLimit={list.setLimit}/></>}</QueryView>
    </Panel>{editor&&<CandidateEditor candidate={editor} onClose={()=>setEditor(null)} onSaved={()=>{toast('Đã lưu thay đổi và audit.');refresh();}}/>}{preview&&<PublishDialog preview={preview} onClose={()=>setPreview(null)} onPublished={()=>{toast('Đã xuất bản lô từ vựng.');setPreview(null);setSelected(new Set());refresh();}}/>}</>;
}

