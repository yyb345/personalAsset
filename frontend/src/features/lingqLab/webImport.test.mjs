import assert from 'node:assert/strict'
import { safeSourceUrl, extractWebArticle } from './webImport.js'
assert.equal(safeSourceUrl('javascript:alert(1)'), '')
assert.equal(safeSourceUrl('data:text/html,test'), '')
assert.equal(safeSourceUrl('file:///etc/passwd'), '')
assert.equal(safeSourceUrl('https://user:password@example.com'), '')
assert.equal(safeSourceUrl(' https://example.com/article '), 'https://example.com/article')
let calls = 0
const signal = new AbortController().signal
const mock = async (path, options) => {
  calls++; assert.equal(path, '/api/learning/web-import'); assert.equal(options.method, 'POST');assert.equal(options.signal, signal)
  assert.deepEqual(JSON.parse(options.body), {url:'https://example.com/article'})
  return {ok:true,json:async()=>({title:' An article ',text:' A model learns. ',sourceUrl:'https://example.com/canonical'})}
}
const article = await extractWebArticle('https://example.com/article', {fetcher:mock,signal})
assert.deepEqual(article,{title:'An article',text:'A model learns.',sourceUrl:'https://example.com/canonical'})
assert.equal(calls,1)
await assert.rejects(extractWebArticle('javascript:alert(1)', {fetcher:mock}), /http/)
assert.equal(calls,1)
await assert.rejects(extractWebArticle('https://example.com', {fetcher:async()=>({ok:false,json:async()=>({error:'页面不允许提取'})})}), /页面不允许提取/)
await assert.rejects(extractWebArticle('https://example.com', {fetcher:async()=>({ok:true,json:async()=>({text:''})})}), /正文/)
await assert.rejects(extractWebArticle('https://example.com', {fetcher:async()=>({ok:true,json:async()=>({text:'a'.repeat(50001)})})}), /50,000/)
await assert.rejects(extractWebArticle('https://example.com', {fetcher:async()=>({ok:true,json:async()=>({text:'A model.',sourceUrl:'javascript:alert(1)'})})}), /来源链接/)
await assert.rejects(extractWebArticle('https://example.com', {fetcher:async()=>({ok:false,json:async()=>{throw new Error()}})}), /无效响应/)
const controller = new AbortController();controller.abort()
await assert.rejects(extractWebArticle('https://example.com',{signal:controller.signal,fetcher:async(_,options)=>{options.signal.throwIfAborted()}}), {name:'AbortError'})
console.log('PASS: safe links, request contract, successful extraction, service errors, invalid/oversized payloads, abort signal')
