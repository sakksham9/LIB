package com.library.dsa;
import com.library.book.Book;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class BookSearchIndex {
 private final Map<String,Book> isbnMap=new ConcurrentHashMap<>();
 private final Trie trie=new Trie(); private final TitleBST bst=new TitleBST();
 public synchronized void rebuild(List<Book> books) {
  isbnMap.clear(); trie.clear(); bst.clear();
  for(Book b:books) { if(b.getIsbn()!=null&&!b.getIsbn().isBlank()) isbnMap.put(b.getIsbn(),b);
   if(b.getTitle()!=null){trie.insert(b.getTitle());bst.insert(b.getTitle());} }
 }
 public Book findByIsbn(String isbn){return isbnMap.get(isbn);}
 public List<String> suggestTitles(String prefix,int limit){return trie.suggest(prefix,limit);}
 public List<String> titlesSorted(){return bst.sorted();}
 static class Trie {
  static class Node {Map<Character,Node> children=new TreeMap<>(); boolean word;}
  Node root=new Node(); void clear(){root=new Node();}
  void insert(String s){Node n=root;for(char c:s.toLowerCase(Locale.ROOT).toCharArray())n=n.children.computeIfAbsent(c,k->new Node());n.word=true;}
  List<String> suggest(String p,int limit){if(p==null||limit<1)return List.of();Node n=root;String key=p.toLowerCase(Locale.ROOT);
   for(char c:key.toCharArray()){n=n.children.get(c);if(n==null)return List.of();}
   List<String> out=new ArrayList<>();collect(n,new StringBuilder(key),out,limit);return out;}
  void collect(Node n,StringBuilder s,List<String> out,int limit){if(out.size()>=limit)return;if(n.word)out.add(s.toString());
   for(var e:n.children.entrySet()){if(out.size()>=limit)break;s.append(e.getKey());collect(e.getValue(),s,out,limit);s.deleteCharAt(s.length()-1);}}
 }
 static class TitleBST {
  static class Node {String v;Node l,r;Node(String v){this.v=v;}}
  Node root;void clear(){root=null;}void insert(String s){root=put(root,s);}
  Node put(Node n,String s){if(n==null)return new Node(s);int c=s.compareToIgnoreCase(n.v);if(c<0)n.l=put(n.l,s);else if(c>0)n.r=put(n.r,s);return n;}
  List<String> sorted(){List<String> out=new ArrayList<>();walk(root,out);return out;}
  void walk(Node n,List<String> out){if(n==null)return;walk(n.l,out);out.add(n.v);walk(n.r,out);}
 }
}
