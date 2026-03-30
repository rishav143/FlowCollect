import{c as y,j as c,r as x}from"./index-BALL9XxK.js";/**
 * @license lucide-react v0.441.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const g=y("Grip",[["circle",{cx:"12",cy:"5",r:"1",key:"gxeob9"}],["circle",{cx:"19",cy:"5",r:"1",key:"w8mnmm"}],["circle",{cx:"5",cy:"5",r:"1",key:"lttvr7"}],["circle",{cx:"12",cy:"12",r:"1",key:"41hilf"}],["circle",{cx:"19",cy:"12",r:"1",key:"1wjl8i"}],["circle",{cx:"5",cy:"12",r:"1",key:"1pcz8c"}],["circle",{cx:"12",cy:"19",r:"1",key:"lyex9k"}],["circle",{cx:"19",cy:"19",r:"1",key:"shf9b7"}],["circle",{cx:"5",cy:"19",r:"1",key:"bfqh0e"}]]);/**
 * @license lucide-react v0.441.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const a=y("LayoutGrid",[["rect",{width:"7",height:"7",x:"3",y:"3",rx:"1",key:"1g98yp"}],["rect",{width:"7",height:"7",x:"14",y:"3",rx:"1",key:"6d4xhi"}],["rect",{width:"7",height:"7",x:"14",y:"14",rx:"1",key:"nxv5o0"}],["rect",{width:"7",height:"7",x:"3",y:"14",rx:"1",key:"1bb6yr"}]]);/**
 * @license lucide-react v0.441.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const h=y("List",[["line",{x1:"8",x2:"21",y1:"6",y2:"6",key:"7ey8pc"}],["line",{x1:"8",x2:"21",y1:"12",y2:"12",key:"rjfblc"}],["line",{x1:"8",x2:"21",y1:"18",y2:"18",key:"c3b1m8"}],["line",{x1:"3",x2:"3.01",y1:"6",y2:"6",key:"1g7gq3"}],["line",{x1:"3",x2:"3.01",y1:"12",y2:"12",key:"1pjlvk"}],["line",{x1:"3",x2:"3.01",y1:"18",y2:"18",key:"28t2mc"}]]),o={list:{icon:c.jsx(h,{size:15,strokeWidth:1.8}),title:"List view"},grid2:{icon:c.jsx(a,{size:15,strokeWidth:1.8}),title:"2-column grid"},grid3:{icon:c.jsx(g,{size:15,strokeWidth:1.8}),title:"3-column grid"}};function u(i,s){const r=`fc-view-${i}`,[e,l]=x.useState(()=>{try{const t=localStorage.getItem(r);if(t&&t in o)return t}catch{}return s}),n=x.useCallback(t=>{l(t);try{localStorage.setItem(r,t)}catch{}},[r]);return[e,n]}function m(i){return i==="grid3"?"grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4":i==="grid2"?"grid grid-cols-1 sm:grid-cols-2 gap-4":"flex flex-col gap-3"}function f({value:i,onChange:s,options:r=["list","grid2","grid3"]}){return c.jsx("div",{className:"flex items-center gap-0.5 p-1 rounded-lg bg-[#F4F7F9] dark:bg-[#243447]",children:r.map(e=>{const{icon:l,title:n}=o[e],t=i===e,d=e==="grid2"||e==="grid3";return c.jsx("button",{title:n,onClick:()=>s(e),className:["w-7 h-7 items-center justify-center rounded-md transition-colors",d?"hidden sm:flex":"flex",t?"bg-white dark:bg-[#1B2838] text-[#0D1B2A] dark:text-white shadow-sm":"text-c-muted hover:text-[#0D1B2A] dark:hover:text-white"].join(" "),children:l},e)})})}export{f as V,m as g,u};
