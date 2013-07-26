/**
 * Copyright (C) 2006 NetMind Consulting Bt.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package hu.netmind.beankeeper.parser;

import hu.netmind.beankeeper.model.*;
import hu.netmind.beankeeper.schema.SchemaManager;
import hu.netmind.beankeeper.type.TypeHandlerTracker;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * The parser resolver. Handles resolving objects and attributes
 * to table names and such. This class also handles the symbol table.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class WhereResolver
{
   private static Logger logger = Logger.getLogger(WhereResolver.class);
  
   private HashMap symbolTable;
   private SymbolTableEntry lastSymbolTableEntry;
   // Main class stuff only used by 'find', when selecting single class
   private SpecifiedTableTerm mainTerm;
   private ClassInfo mainClassInfo;
   private ClassSpecifier mainClassSpecifier;

   private SchemaManager schemaManager = null;
   private ClassTracker classTracker = null;
   private TypeHandlerTracker typeHandlerTracker = null;

   public WhereResolver(ClassTracker classTracker, TypeHandlerTracker typeHandlerTracker,
         SchemaManager schemaManager)
   {
      this.classTracker=classTracker;
      this.typeHandlerTracker=typeHandlerTracker;
      this.schemaManager=schemaManager;
      symbolTable = new HashMap();
      mainTerm = null;
      mainClassInfo = null;
   }
   
   /**
    * Resolve main table name.
    */
   public TableTerm resolve(ClassSpecifier specifier, boolean selected)
   {
      String alias = specifier.getAlias()!=null?specifier.getAlias():specifier.getClassName();
      // Insert symbol table entry
      SymbolTableEntry entry = (SymbolTableEntry) symbolTable.get(alias);
      if ( entry == null )
      {
         // Determine class for specifier
         ClassInfo classInfo = classTracker.getClassInfo(
               classTracker.getMatchingClassEntry(specifier.getClassName()));
         if ( classInfo == null )
            throw new ParserException(ParserException.SYMBOL_ERROR,"could not find class for table name: "+specifier.getClassName());
         // Entry was null, create
         entry = new SymbolTableEntry();
         entry.specifiedTerm = new SpecifiedTableTerm(
               schemaManager.getTableName(classInfo.getSourceEntry()), specifier.getAlias());
         entry.automatic = false;
         entry.expression = null;
         entry.selected=true;
         entry.classInfo = classInfo;
         symbolTable.put(alias,entry);
         lastSymbolTableEntry=entry;
         if ( specifier.getAlias() == null )
         {
            // Insert default table name symbol too, for later left table
            // term fixing, which will use the full table name
            symbolTable.put(entry.specifiedTerm.getTableName(),entry);
         }
         logger.debug("adding table to symbol table: "+entry.specifiedTerm);
         // Remember main table
         if ( mainTerm == null )
         {
            logger.debug("table entry "+entry.specifiedTerm+" is selected, adding left tables.");
            // Remember
            mainClassSpecifier = specifier;
            mainClassInfo = classInfo;
            mainTerm = entry.specifiedTerm; // Main term is the selected class/table
            // Fix entry to contain all relevant classes
            if ( selected )
               fixLeftTableTerms(entry.specifiedTerm,entry);
         }
      }
      // Construct table term
      TableTerm tableTerm = new TableTerm(entry.specifiedTerm);
      // Return with table term
      logger.debug("translated: "+specifier+" -> "+tableTerm);
      return tableTerm;
   }

   private ReferenceTerm getReferenceTerm(SymbolTableEntry entry)
   {
      switch ( entry.type )
      {
         case SymbolTableEntry.TYPE_HANDLED:
            return new ReferenceTerm(entry.specifiedTerm,entry.referenceColumn);
         case SymbolTableEntry.TYPE_OBJECT:
            return new ReferenceTerm(entry.specifiedTerm,"persistence_id");
         default:
            return null;
      }
   }

   /**
    * Resolver attribute chain.
    */
   public ReferenceTerm resolve(ClassSpecifier specifier, List attributeSpecifiers)
   {
      // Resolve class specifier
      // This is not as easy as it sounds: if there are no attribute
      // specifiers and there is no alias given, it may mean that
      // this identifier represents an attribute to selected main class
      // (if there is one).
      if ( ((attributeSpecifiers==null) || (attributeSpecifiers.size()==0)) 
            && (specifier.getAlias()==null) && (mainClassInfo!=null) )
      {
         String attributeName = specifier.getClassName();
         if ( mainClassInfo.getAttributeType(attributeName) != null )
         {
            // Simulate a normal entry
            specifier = mainClassSpecifier;
            attributeSpecifiers = new ArrayList();
            attributeSpecifiers.add(new AttributeSpecifier(attributeName,null,null));
         }
      }
      // Now go through names, the root name does already exist,
      // because that is the class. All other names are 
      // combined with attributes (book.title). This also should
      // be inserted into symbol table, if it is a class.
      resolve(specifier,false); // Resolve class
      StringBuffer alias = new StringBuffer(specifier.getAlias()==null?specifier.getClassName():specifier.getAlias());
      SymbolTableEntry previousEntry = (SymbolTableEntry) symbolTable.get(alias.toString());
      ClassInfo previousInfo = previousEntry.classInfo;
      AttributeSpecifier primitiveAttributeSpecifier = null;
      for ( int i=0; (attributeSpecifiers!=null) && (i<attributeSpecifiers.size()); i++ )
      {
         AttributeSpecifier spec = (AttributeSpecifier) attributeSpecifiers.get(i);
         String attributeName = spec.getIdentifier();
         Class attributeClass = previousInfo.getAttributeType(attributeName);
         if ( attributeClass == null )
            throw new ParserException(ParserException.ABORT,"can not resolve the identifier '"+
                  attributeName+"' on '"+alias.toString()+"', classinfo: "+previousInfo);
         alias.append("."+attributeName);
         logger.debug("processing attribute: "+alias);
         ClassTracker.ClassType attributeType = classTracker.getType(attributeClass);
         // Handle additional array specifier.
         if ( (spec.getKeyname() != null) && (attributeType != ClassTracker.ClassType.TYPE_HANDLED) )
               throw new ParserException(ParserException.ABORT,"additional array specifier found, but type is not 'handled': "+alias);
         if ( attributeType == ClassTracker.ClassType.TYPE_HANDLED )
         {
            logger.debug("attribute '"+alias+"' found to be a handled type, letting the handler in.");
            // If the array specifier is given, add to alias
            if ( spec.getKeyname() != null )
               alias.append("['"+spec.getKeyname()+"']");
            // Calculate the symbol table entry for this part
            SymbolTableEntry entry = (SymbolTableEntry) symbolTable.get(alias.toString());
            if ( entry == null )
            {
               entry = typeHandlerTracker.
                  getHandler(attributeClass).getSymbolEntry(spec,previousEntry,
                        previousInfo,getReferenceTerm(previousEntry));
               if ( entry == null )
               {
                  // Entry was not created, this means this is a primitive type
                  logger.debug("handled attribute '"+alias+"' found to be a primitive attribute.");
                  // A primitive type should be the last in the list
                  if ( i+1 < attributeSpecifiers.size() )
                     throw new ParserException(ParserException.ABORT,"A handled primitive type encountered, but not the last in list: "+alias.toString());
                  // Mark primitive type
                  primitiveAttributeSpecifier = spec;
                  entry = previousEntry; // It's entry is it's parent
               } else if ( entry.specifiedTerm.getAlias() != null ) {
                  // Entry wants to be in the symbol table
                  symbolTable.put(entry.specifiedTerm.getAlias(),entry);
                  lastSymbolTableEntry=entry;
               } else {
                  // Entry does no want into the symbol table,
                  // so create an extremal symbol.
                  int num = 0;
                  while ( symbolTable.containsKey(alias.toString()+"-notused"+num) )
                     num++;
                  symbolTable.put(alias.toString()+"-notused"+num,entry);
                  lastSymbolTableEntry=entry;
               }
            }
            previousEntry = entry;
            // If this is not the last specifier, then calculate the
            // class info for the next specifier.
            if ( i+1 < attributeSpecifiers.size() )
               previousInfo = typeHandlerTracker.
                  getHandler(attributeClass).getSymbolInfo(entry,spec);
         }
         // Reserved type
         if ( attributeType == ClassTracker.ClassType.TYPE_RESERVED )
            throw new ParserException(ParserException.ABORT,"attribute type is reserved, can not handle it: "+alias.toString());
         // Attribute is an object
         if ( attributeType == ClassTracker.ClassType.TYPE_OBJECT )
         {
            ClassInfo containerInfo = previousInfo;
            if ( spec.getClassName() != null )
               previousInfo = classTracker.getClassInfo(
                     classTracker.getMatchingClassEntry(spec.getClassName()));
            else
               previousInfo = classTracker.getClassInfo(previousInfo.getAttributeType(attributeName),null);
            if ( logger.isDebugEnabled() )
               logger.debug("attribute '"+alias+"' found to be an object, with entry: "+previousInfo.getSourceEntry());
            // If the attribute is non-storable, then it will get flagged in
            // fixNonstorableTerms() method. But here, we let it through, because
            // maybe, it is compared to a primitive type, in which case, we still
            // can make a meaningful expression out of it (in fixPrimitiveExpression)
            if ( ! previousInfo.isStorable() )
            {
               ReferenceTerm idTerm = new ReferenceTerm(
                     previousEntry.specifiedTerm,attributeName);
               previousEntry.termList.add(idTerm);
               if ( logger.isDebugEnabled() )
                  logger.debug("attribute '"+alias+"' refers to a non-storable object, so reference will be: "+idTerm);
               return idTerm;
            }
            // Get entry
            SymbolTableEntry entry = (SymbolTableEntry) symbolTable.get(alias.toString());
            if ( entry == null )
            {
               // Create entry
               entry = new SymbolTableEntry();
               entry.specifiedTerm = new SpecifiedTableTerm(
                     schemaManager.getTableName(previousInfo.getSourceEntry()),null);
               entry.automatic = true;
               entry.type = SymbolTableEntry.TYPE_OBJECT;
               entry.classInfo = previousInfo;
               symbolTable.put(alias.toString(),entry);
               lastSymbolTableEntry=entry;
               // Create expression
               Expression expr = new Expression();
               // Determine whether the entry's tableName is correct, or if
               // a superclass's table is required for attribute
               TableTerm previousTerm = new TableTerm(previousEntry.specifiedTerm);
               ClassEntry superClassEntry = containerInfo.getAttributeClassEntry(attributeName);
               if ( logger.isDebugEnabled() )
                  logger.debug("selecting object attribute "+attributeName+", class: "+
                        previousEntry.classInfo.getSourceEntry()+", superclass: "+superClassEntry);
               if ( ! previousEntry.classInfo.getSourceEntry().equals(superClassEntry) )
               {
                  // Class holding the attribute differs from entry's class.
                  // Note: if a superclass is in charge, there should be no
                  // dynamic name.
                  ClassInfo superInfo = classTracker.getClassInfo(superClassEntry);
                  String superTableName = schemaManager.getTableName(superClassEntry);
                  previousTerm = new TableTerm(superTableName,null);
                  if ( logger.isDebugEnabled() )
                     logger.debug("determining whether to allocate supertable: "+previousTerm+", tables allocated: "+previousEntry.allocatedSuperTerms);
                  if ( ! previousEntry.allocatedSuperTerms.contains(previousTerm) )
                  {
                     logger.debug("allocating superclass: "+superClassEntry);
                     // This superclass was not yet allocated, so add
                     // as a related left term
                     Expression joinExpr = new Expression();
                     ReferenceTerm connectLeftTerm = new ReferenceTerm(previousEntry.specifiedTerm,"persistence_id");
                     previousEntry.termList.add(connectLeftTerm);
                     joinExpr.add(connectLeftTerm);
                     joinExpr.add("=");
                     ReferenceTerm connectRightTerm = new ReferenceTerm(previousTerm,"persistence_id");
                     previousEntry.termList.add(connectRightTerm);
                     joinExpr.add(connectRightTerm);
                     SpecifiedTableTerm.LeftjoinEntry leftEntry =  new SpecifiedTableTerm.LeftjoinEntry();
                     leftEntry.term=previousTerm;
                     leftEntry.expression=joinExpr;
                     previousEntry.specifiedTerm.getRelatedLeftTerms().add(leftEntry);
                     // Add to list
                     previousEntry.allocatedSuperTerms.add(previousTerm);
                  }
               }
               // Now join the term to the parent term
               if ( i+1 >= attributeSpecifiers.size() )
               {
                  // Object is the last in the reference list, so it is not
                  // dereferenced. In this case, we must provide a column
                  // of persistence_id, which is null if the parent object's
                  // attribute is null, _or_ if it's not null, but the object
                  // is deleted. To do this, we only left-join the table of
                  // the type, so we will get a persistence_id anyway.
                  ReferenceTerm refTerm = getReferenceTerm(entry);
                  entry.termList.add(refTerm);
                  entry.expression=expr;
                  entry.leftTerm=true; // Mark as not standalone
                  // Add leftjoin entry
                  SpecifiedTableTerm.LeftjoinEntry leftEntry = new SpecifiedTableTerm.LeftjoinEntry();
                  leftEntry.term=refTerm;
                  Expression joinExpr = new Expression();
                  leftEntry.expression=joinExpr;
                  ReferenceTerm toTerm = new ReferenceTerm(previousTerm,attributeName);
                  previousEntry.termList.add(toTerm);
                  joinExpr.add(toTerm);
                  joinExpr.add("=");
                  ReferenceTerm valueTerm = new ReferenceTerm(refTerm,"persistence_id");
                  joinExpr.add(valueTerm);
                  entry.termList.add(valueTerm);
                  previousEntry.specifiedTerm.getReferencedLeftTerms().add(leftEntry);
               } else {
                  // Object is not the last in the reference list, meaning it is
                  // dereferenced, which in turn means, that it must be hard-joined
                  // so all the attributes exist, or the select fails if the object
                  // can not be joined
                  ReferenceTerm leftTerm = new ReferenceTerm(previousTerm,attributeName);
                  previousEntry.termList.add(leftTerm);
                  if ( ! expr.isEmpty() )
                     expr.add("and");
                  expr.add(leftTerm);
                  expr.add("=");
                  ReferenceTerm refTerm = getReferenceTerm(entry);
                  expr.add(refTerm);
                  entry.termList.add(refTerm);
                  entry.expression=expr;
               }
            }
            previousEntry = entry;
         }
         // Attribute is primitive type
         if ( attributeType == ClassTracker.ClassType.TYPE_PRIMITIVE )
         {
            logger.debug("attribute '"+alias+"' found to be a primitive attribute.");
            // A primitive type should be the last in the list
            if ( i+1 < attributeSpecifiers.size() )
               throw new ParserException(ParserException.ABORT,"A primitive type encountered, but not the last in list: "+alias.toString());
            // Mark primitive type
            primitiveAttributeSpecifier = spec;
         }
      }
      // If we're here, that means, attribute list has ended.
      // Either in a primitive type attribute or in object. 
      SymbolTableEntry entry = (SymbolTableEntry) symbolTable.get(alias.toString());
      if ( entry == null )
      {
         // This means we are in an attribute, so get previous entry
         entry = previousEntry;
      }
      // Construct and return term
      ReferenceTerm result = null;
      if ( primitiveAttributeSpecifier != null )
      {
         // Attribute specified, this means, we must check whether
         // this attribute is part of the class in entry or in superclass.
         // "persistenceid" is considered part of every class, so no superclasses
         // are selected for that.
         TableTerm tableTerm = new TableTerm(entry.specifiedTerm);
         ClassEntry superClassEntry = entry.classInfo.getAttributeClassEntry(primitiveAttributeSpecifier.getIdentifier());
         if ( logger.isDebugEnabled() )
            logger.debug("selecting primitive attribute "+primitiveAttributeSpecifier.getIdentifier()+
                  ", class: "+entry.classInfo.getSourceEntry()+", superclass: "+superClassEntry);
         if ( (! entry.classInfo.getSourceEntry().equals(superClassEntry)) &&
           (!"persistenceid".equalsIgnoreCase(primitiveAttributeSpecifier.getIdentifier())) )
         {
            // Class holding the attribute differs from entry's class
            ClassInfo superInfo = classTracker.getClassInfo(superClassEntry);
            String superTableName = schemaManager.getTableName(superClassEntry);
            tableTerm = new TableTerm(superTableName,null);
            if ( ! entry.allocatedSuperTerms.contains(tableTerm) )
            {
               logger.debug("primitive attribute is a related class' attribute, and not yet allocated.");
               // This superclass was not yet allocated
               Expression joinExpr = new Expression();
               ReferenceTerm connectLeftTerm = new ReferenceTerm(entry.specifiedTerm,"persistence_id");
               entry.termList.add(connectLeftTerm);
               joinExpr.add(connectLeftTerm);
               joinExpr.add("=");
               ReferenceTerm connectRightTerm = new ReferenceTerm(tableTerm,"persistence_id");
               entry.termList.add(connectRightTerm);
               joinExpr.add(connectRightTerm);
               SpecifiedTableTerm.LeftjoinEntry leftEntry =  new SpecifiedTableTerm.LeftjoinEntry();
               leftEntry.term=tableTerm;
               leftEntry.expression=joinExpr;
               entry.specifiedTerm.getRelatedLeftTerms().add(leftEntry);
               // Add to list
               entry.allocatedSuperTerms.add(tableTerm);
            }
         }
         // Construct result
         String attributeName = primitiveAttributeSpecifier.getIdentifier();
         boolean id = false;
         if ( "persistenceid".equalsIgnoreCase(attributeName) )
         {
            attributeName = "persistence_id";
            id = true;
         }
         result = new ReferenceTerm(tableTerm,attributeName);
         if ( id ) // Mark specifically for id reference
            result.setId();
      } else {
         // Term did not end in attribute specification, so leave
         // persistence id, which is available in all tables
         result = getReferenceTerm(entry);
      }
      entry.termList.add(result);
      return result; // Return term
   }

   /**
    * Fix time control to apply transaction time too, if the statement
    * has common tables with transaction modified tables.
    */
   public void fixTimeControl(QueryStatement stmt, Set modifiedTables)
   {
      if ( stmt.getTimeControl().isApplyTransaction() )
         return;
      // Go through all entries and whatch for tables
      Iterator entryIterator = symbolTable.values().iterator();
      while ( entryIterator.hasNext() )
      {
         SymbolTableEntry entry = (SymbolTableEntry) entryIterator.next();
         boolean found = false;
         if ( modifiedTables.contains(entry.specifiedTerm.getTableName()) )
            found=true;
         for ( int i=0; i<entry.specifiedTerm.getRelatedLeftTerms().size(); i++ )
            if ( modifiedTables.contains(((SpecifiedTableTerm.LeftjoinEntry)
                        entry.specifiedTerm.getRelatedLeftTerms().get(i)).term.getTableName()) )
               found=true;
         for ( int i=0; i<entry.specifiedTerm.getReferencedLeftTerms().size(); i++ )
            if ( modifiedTables.contains(((SpecifiedTableTerm.LeftjoinEntry)
                        entry.specifiedTerm.getReferencedLeftTerms().get(i)).term.getTableName()) )
               found=true;
         // If found a common table, then set transaction to apply
         // and return
         if ( found )
         {
            stmt.getTimeControl().setApplyTransaction(true);
            return; // Transaction applies, common table found
         }
      }
   }

   /**
    * Fix atomic expressions for 2VL (2 valued logic), instead of sql 3VL.
    * This means, that while "a.attr = b.attr" will not work in sql when
    * both are null, BeanKeeper supposes, that a developer wants that expression
    * to evaluate to 'true', when both attributes are null.
    */
   private Expression fixThreeValuedLogicExpressions(ReferenceTerm term1,
         ReferenceTerm term2, String op)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("checking for 3VL in following: "+term1+" "+op+" "+term2);
      Expression result = new Expression();
      // Check whether both terms refer to non-primitve types, because if at
      // least one is primitive, then they can't be null anyway
      ClassInfo info1 = classTracker.getClassInfo(schemaManager.getClassEntry(term1.getTableName()));
      ClassInfo info2 = classTracker.getClassInfo(schemaManager.getClassEntry(term2.getTableName()));
      if ( (!term1.getColumnName().equals("persistence_id")) &&
           (!term2.getColumnName().equals("persistence_id")) &&
           ( (info1.getAttributeType(term1.getColumnName()).isPrimitive()) ||
             (info2.getAttributeType(term2.getColumnName()).isPrimitive())) )
      {
         // At least one term refers to a primitive type, so that can't be null,
         // no 3VL logic will interfere with operator
         result.add(term1);
         result.add(op);
         result.add(term2);
         return result;
      }
      // Both terms are referring to non-primitive types, so modify them
      // to support 'null' comparisons
      result.setMarkedBlock(true); // Should be in paranthesis
      if ( op.equals("=") )
      {
         // We need to create the following expression:
         // ( term1=term2 or term1 is null and term2 is null )
         result.add(term1);
         result.add("=");
         result.add(term2);
         result.add("or");
         result.add(term1);
         result.add("is null");
         result.add("and");
         result.add(term2);
         result.add("is null");
      } else {
         // We need the following (why is xor missing from sql?):
         // ( term1<>term2 or term1 is null and term2 is not null or 
         // term1 is not null and term2 is null )
         result.add(term1);
         result.add("<>");
         result.add(term2);
         result.add("or");
         result.add(term1);
         result.add("is null");
         result.add("and");
         result.add(term2);
         result.add("is not null");
         result.add("or");
         result.add(term1);
         result.add("is not null");
         result.add("and");
         result.add(term2);
         result.add("is null");
      }
      return result;
   }

   /**
    * Fix atomic (2 terms, 1 operator) expression artifacts.
    */
   public Expression fixAtomicExpressions(Object term1, Object term2, String op)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("fixing atomic expression: "+term1+" <"+op+"> "+term2);
      // If there is a constant term present, then select it. If
      // they are both constant terms, then same logic still applies.
      Expression result = null;
      Object constantTerm = term2;
      Object otherTerm = term1;
      if ( term1 instanceof ConstantTerm )
      {
         constantTerm = term1;
         otherTerm = term2;
      }
      // Check whether there is a constant term, which is primitive. In this case
      // we must check, that it's a primitive or not, because
      // in this case we may be forced to alter the other term.
      if ( (otherTerm instanceof ReferenceTerm) &&
           (constantTerm instanceof ConstantTerm) && 
           (((ConstantTerm) constantTerm).getValue()!=null) )
      {
         result = fixPrimitiveExpression((ReferenceTerm)otherTerm,
               (ConstantTerm)constantTerm,op);
         return result;
      }
      // If operator is not equals or not-equals, then 3VL conversion is not applicable
      if ( (!op.equals("=")) && (!op.equals("!=")) && (!op.equals("<>")) )
      {
         result = new Expression();
         result.add(term1);
         result.add(op);
         result.add(term2);
         return result;
      }
      // Do 3VL convert logic
      if ( constantTerm instanceof ConstantTerm )
      {
         // There is at least one constant term in the expression
         if ( ((ConstantTerm)constantTerm).getValue()==null )
         {
            // At least one constant term is null in the expression,
            // so change the operator to fit sql 3VL
            result = new Expression();
            result.add(otherTerm);
            if ( op.equals("=") )
               result.add("is null");
            else
               result.add("is not null");
         }
      } else {
         // Both terms are referenceterms, check 3VL between them
         if ( (term1 instanceof ReferenceTerm) && (term2 instanceof ReferenceTerm) )
            result = fixThreeValuedLogicExpressions((ReferenceTerm) term1,
                  (ReferenceTerm) term2, op);
      }
      return result;
   }

   /**
    * Fix a primitive expression.
    * The problem is expressions like this: find holder where holder.attr = 'Ni'.
    * If the holder.attr is an object type, and not declared 'primitive'
    * (in which case it still can hold primitive boxed types), then
    * the parser can not know it is meant to be primitive only when it
    * comes to the right term which is primitive constant. So in this case, we
    * must alter the expression to include the boxed primitive type's table.
    */
   private Expression fixPrimitiveExpression(ReferenceTerm leftTerm, 
         ConstantTerm rightTerm, String op)
   {
      // Make the prototype expression
      Expression result = new Expression();
      result.add(leftTerm);
      result.add(op);
      result.add(rightTerm);
      // Make a few checks
      if ( classTracker.getType(rightTerm.getValue().getClass()) !=
            ClassTracker.ClassType.TYPE_PRIMITIVE )
         return result; // Not a primitive constant, so there no point
      if ( rightTerm.isId() || leftTerm.isId() )
         return result; // Is an Id expression specifically, and not primitive boxes types
      ClassInfo leftInfo = classTracker.getClassInfo(schemaManager.getClassEntry(leftTerm.getTableName()));
      if ( (leftInfo!=null) && 
            (classTracker.getType(leftInfo.getAttributeType(leftTerm.getColumnName()))
             == ClassTracker.ClassType.TYPE_PRIMITIVE) )
         return result; // Attribute is primitive, so no modification needed for checking
      if ( logger.isDebugEnabled() )
         logger.debug("fixing primitive expression: "+result);
      // Now insert expression referencing the boxed primitive type's table
      // Note: We must insert a symbol table entry for the primitive type's
      // table, but it's not a real symbol, it should have an extremal
      // name.
      ClassInfo primitiveInfo = classTracker.getClassInfo(rightTerm.getValue().getClass(),rightTerm.getValue());
      String primitiveTableName = schemaManager.getTableName(primitiveInfo.getSourceEntry());
      TableTerm primitiveTableTerm = new TableTerm(primitiveTableName,null);
      SymbolTableEntry entry = new SymbolTableEntry();
      entry.specifiedTerm=new SpecifiedTableTerm(primitiveTableName,null);
      entry.automatic = true;
      entry.type = SymbolTableEntry.TYPE_OBJECT;
      symbolTable.put("primitive"+symbolTable.size(),entry);
      // Create contact expression to primitive table
      Expression contactExpr = new Expression();
      contactExpr.add(leftTerm);
      contactExpr.add("=");
      ReferenceTerm contactTerm = new ReferenceTerm(primitiveTableTerm,"persistence_id");
      contactExpr.add(contactTerm);
      entry.termList.add(contactTerm);
      entry.expression=contactExpr;
      // Replace the reference term with new primitive term
      ReferenceTerm valueTerm = new ReferenceTerm(primitiveTableTerm,"value");
      entry.termList.add(valueTerm);
      result.set(0,valueTerm);
      return result;
   }


   private boolean fixContainsNegated(Expression expr, boolean isNegated)
   {
      if ( expr == null )
         return false;
      boolean originalNegated = isNegated;
      isNegated=false;
      for ( int i=0; i<expr.size(); i++ )
      {
         Object obj = expr.get(i);
         if (obj instanceof String)
         {
            if ( "contains".equalsIgnoreCase((String) obj) )
            {
               if ( isNegated ^ originalNegated )
                  return true;
               expr.set(i,"="); // Replace with '=' sign
            } else
               isNegated = "not".equalsIgnoreCase((String) obj);
         }
         if ( (obj instanceof Expression ) && 
               (fixContainsNegated((Expression) obj,isNegated ^ originalNegated)) )
            return true;
      }
      return false;
   }

   /**
    * Fix unaliased generated table term.
    */
   private Expression fixAutomaticTerms(Expression expr)
   {
      // First, generate all temporary names
      Expression plusExpression = new Expression();
      String namePrefix = "t_";
      int tempNameIndex = 1;
      // Entries will be filtered through a set, because a single
      // entry could be present in the map multiple times (for example
      // a main table is available as an alias, and as a table name entry)
      Iterator iterator = new HashSet(symbolTable.values()).iterator();
      while ( iterator.hasNext() )
      {
         SymbolTableEntry entry = (SymbolTableEntry) iterator.next();
         // Generate index for class and all superclasses!
         HashMap tableNameAliases = new HashMap();
         ArrayList tableTerms = new ArrayList(entry.allocatedSuperTerms);
         if ( entry.automatic )
            tableTerms.add(entry.specifiedTerm);
         for ( int i=0; i<tableTerms.size(); i++ )
         {
            TableTerm term = (TableTerm) tableTerms.get(i);
            while ( symbolTable.get(namePrefix+tempNameIndex) != null)
               tempNameIndex++;
            tableNameAliases.put(term.getTableName(),namePrefix+tempNameIndex);
            term.setAlias(namePrefix+tempNameIndex);
            tempNameIndex++;
         }
         // Walk through referred terms and fill in the gaps
         for ( int i=0; i<entry.termList.size(); i++ )
         {
            TableTerm term = (TableTerm) entry.termList.get(i);
            String newAlias = (String) tableNameAliases.get(term.getTableName());
            if ( newAlias != null )
               term.setAlias(newAlias);
         }
         // Remember it's expression
         if ( (entry.expression != null) && (entry.expression.size()>0) )
         {
            if ( logger.isDebugEnabled() )
               logger.debug("entry '"+entry.specifiedTerm+"' has connector expression: "+entry.expression);
            if ( plusExpression.size() > 0 )
               plusExpression.add("and");
            plusExpression.addAll(entry.expression);
         }
      }
      // Now create final expression
      Expression result = expr;
      if ( plusExpression.size() != 0 )
      {
         if ( result.size() != 0 )
         {
            result = new Expression();
            result.add(plusExpression);
            result.add("and");
            result.add(expr);
         } else {
            result = plusExpression;
         }
      }
      // Return
      return result;
   }

   /**
    * Add all left table terms to term, all terms are at this point fixed.
    * Note that method does not directly use the related classes returned 
    * by the classtracker, because the supplied leftterms already may
    * contain aliases that couldn't be recovered elsewhere.
    */
   private void fixLeftTableTerms(ClassInfo info, SpecifiedTableTerm term, List leftTerms)
   {
      // Calculate left table terms
      Set relatedClassEntries = new HashSet(classTracker.getRelatedClassEntries(info.getSourceEntry()));
      for ( int i=0; i<leftTerms.size(); i++ )
      {
         // Go through left term, if this is a left term to the info, then
         // insert it to this term's left terms.
         // Note: class info can be null, if creating the table for that class
         // previously failed.
         TableTerm leftTerm = ((SpecifiedTableTerm.LeftjoinEntry) leftTerms.get(i)).term;
         ClassInfo classInfo = classTracker.getClassInfo(schemaManager.getClassEntry(leftTerm.getTableName()));
         if ( classInfo != null )
         {
            ClassEntry leftEntry = classInfo.getSourceEntry();
            if ( relatedClassEntries.contains(leftEntry) )
            {
               SpecifiedTableTerm.LeftjoinEntry entry = new SpecifiedTableTerm.LeftjoinEntry();
               entry.term=leftTerm;
               term.getRelatedLeftTerms().add(entry);
               Expression joinExpr = new Expression();
               joinExpr.add(new ReferenceTerm(term,"persistence_id"));
               joinExpr.add("=");
               joinExpr.add(new ReferenceTerm(leftTerm,"persistence_id"));
               entry.expression=joinExpr;
            }
         }
      }
   }
   
   /**
    * Get the left table terms, which will contained in the select. These are all
    * super-, and sub-classes of the specified term.  This method can be used on the
    * selected term, which has to include all of these to return all relevant attributes.
    * Note: all related classes will be included in the allocatedSuperTerms attribute,
    * which is not correct, since it contains subtables too.
    */
   private void fixLeftTableTerms(SpecifiedTableTerm term, SymbolTableEntry mainEntry)
   {
      // Calculate left table terms
      List relatedClassEntries = classTracker.getRelatedClassEntries(mainEntry.classInfo.getSourceEntry());
      if ( logger.isDebugEnabled() )
         logger.debug("found related classes: "+relatedClassEntries+", to class info: "+mainEntry.classInfo);
      for ( int i=0; i<relatedClassEntries.size(); i++ )
      {
         ClassEntry relatedClassEntry = (ClassEntry) relatedClassEntries.get(i);
         ClassInfo relatedClassInfo = classTracker.getClassInfo(relatedClassEntry);
         if ( logger.isDebugEnabled() )
            logger.debug("found left joined class: "+relatedClassEntry+", class info: "+relatedClassInfo);
         if ( relatedClassInfo == null )
            throw new ParserException(ParserException.ABORT,"object class not found for loading: '"+relatedClassEntry+"'");
         // Create the term and add to mainterm
         TableTerm leftTerm = new TableTerm(schemaManager.getTableName(relatedClassEntry),null);
         if ( logger.isDebugEnabled() )
            logger.debug("adding left term to: "+mainEntry.specifiedTerm+", left term: "+leftTerm);
         mainEntry.termList.add(leftTerm);
         mainEntry.allocatedSuperTerms.add(leftTerm);
         // Add left join entry to the specified term
         SpecifiedTableTerm.LeftjoinEntry entry = new SpecifiedTableTerm.LeftjoinEntry();
         mainEntry.specifiedTerm.getRelatedLeftTerms().add(entry);
         entry.term=leftTerm;
         Expression joinExpr = new Expression();
         entry.expression=joinExpr;
         ReferenceTerm toTerm = new ReferenceTerm(term,"persistence_id");
         mainEntry.termList.add(toTerm);
         joinExpr.add(toTerm);
         joinExpr.add("=");
         ReferenceTerm valueTerm = new ReferenceTerm(leftTerm,"persistence_id");
         mainEntry.termList.add(valueTerm);
      }
   }
   
   /**
    * Add date constraint expressions.
    */
   private Expression fixDateConstraints(Collection localAllTableTerms, Expression expr, TimeControl timeControl)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("fixing date constraints for tables: "+localAllTableTerms);
      Expression dateExpression = new Expression();
      // Generate date constraints for all tables which are not left-join tables
      Iterator tablesIterator = localAllTableTerms.iterator();
      while ( tablesIterator.hasNext() )
      {
         SpecifiedTableTerm term = (SpecifiedTableTerm) tablesIterator.next();
         // Add to date constraints
         if ( logger.isDebugEnabled() )
            logger.debug("adding date constraint to term: "+term);
         if ( dateExpression.size() != 0 )
            dateExpression.add("and");
         timeControl.apply(dateExpression,term);
         // Fix term's left join expressions too
         for ( int o=0; o<term.getRelatedLeftTerms().size(); o++ )
         {
            SpecifiedTableTerm.LeftjoinEntry entry = (SpecifiedTableTerm.LeftjoinEntry) term.getRelatedLeftTerms().get(o);
            entry.expression.add("and");
            timeControl.apply(entry.expression,entry.term);
            if ( logger.isDebugEnabled() )
               logger.debug("adding date constraint to left term: "+entry.term);
         }
         for ( int o=0; o<term.getReferencedLeftTerms().size(); o++ )
         {
            SpecifiedTableTerm.LeftjoinEntry entry = (SpecifiedTableTerm.LeftjoinEntry) term.getReferencedLeftTerms().get(o);
            entry.expression.add("and");
            timeControl.apply(entry.expression,entry.term);
            if ( logger.isDebugEnabled() )
               logger.debug("adding date constraint to left join term term: "+entry.term);
         }
      }
      // Return
      Expression result = expr;
      if ( dateExpression.size() != 0 )
      {
         if ( (result!=null) && (result.size() != 0) )
         {
            result = new Expression();
            result.add(dateExpression);
            result.add("and");
            result.add(expr);
         } else {
            result = dateExpression;
         }
      }
      // Return
      return result;
   }

   private boolean validViewSelectTables(QueryStatement stmt)
   {
      if ( stmt.getMode() == QueryStatement.MODE_FIND )
         return true; // Valid, because it is in find mode
      // Check whether all selected terms are storable
      for ( int i=0; i<stmt.getSelectTerms().size(); i++ )
      {
         TableTerm term = (TableTerm) stmt.getSelectTerms().get(i);
         ClassInfo info = classTracker.getClassInfo(schemaManager.getClassEntry(term.getTableName()));
         if ( ! info.isStorable() )
            return false;
      }
      return true;
   }

   private void fixNonstorableTerms(List selectTerms, Expression expr)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("fixing nonstorable terms in expression: "+expr);
      // Go through all terms, and those which are not storable and
      // not selected should cause an error. This is because since there is
      // no ids table, we can not determine what to write in the place of
      // these terms. On some occasions, it would be possible to substitute
      // them, but not generally.
      // Note: this didn't work correctly with ids table either, because
      // the time control was not used in these cases! (On the abstract
      // object)
      if ( expr == null )
         return;
      for ( int i=0; i<expr.size(); i++ )
      {
         Object value = expr.get(i);
         if ( value instanceof Expression )
         {
            fixNonstorableTerms(selectTerms,(Expression) value);
         } else if ( value instanceof TableTerm ) {
            // Found a table term
            TableTerm term = (TableTerm) value;
            ClassInfo info = classTracker.getClassInfo(schemaManager.getClassEntry(term.getTableName()));
            if ( info == null )
               continue; // No worries, most likely an internal table
            ClassEntry entry = info.getSourceEntry();
            if ( (!selectTerms.contains(term)) && (!info.isStorable()) )
            {
               // A non-storable entry's term found which is not selected,
               // so throw error.
               throw new ParserException(ParserException.ABORT,"found non-storable, non-selected term: '"+term+"'. "+
                     "Non-storable classes can not be used if they are not the subject of the select.");
            }
         }
      }
   }

   private void fixOrderBys(QueryStatement stmt)
   {
      if ( stmt.getOrderByList() == null )
         stmt.setOrderByList(new ArrayList());
      if ( stmt.getMode() == QueryStatement.MODE_FIND )
      {
         // Find mode: add default order by on persistence_id
         TableTerm mainTerm = (TableTerm) stmt.getSelectTerms().get(0);
         OrderBy orderBy = new OrderBy(new ReferenceTerm(mainTerm,"persistence_id"),OrderBy.ASCENDING);
         if ( ! stmt.getOrderByList().contains(orderBy) )
            stmt.getOrderByList().add(orderBy);
      } else {
         // View mode: add all view attributes as order bys, so
         // listing becomes unambigous.
         for ( int i=0; i<stmt.getSelectTerms().size(); i++ )
         {
            ReferenceTerm term = (ReferenceTerm) stmt.getSelectTerms().get(i);
            OrderBy orderBy = new OrderBy(term,OrderBy.ASCENDING);
            if ( ! stmt.getOrderByList().contains(orderBy) )
               stmt.getOrderByList().add(orderBy);
         }
      }
   }

   /**
    * Generate the final statements.
    */
   public QueryStatementList generate(QueryStatement stmt)
   {
      Expression expr = stmt.getQueryExpression();
      if ( expr == null )
         expr = new Expression();
      if ( logger.isDebugEnabled() )
      {
         logger.debug("symbol table before finalizing: "+symbolTable);
         logger.debug("expression before finalizing: "+expr);
      }
      // Add default order bys
      fixOrderBys(stmt);
      // Check whether 'contains' operator is negated (this is not allowed)
      // if it does not, then substitute with '=' sign.
      if ( fixContainsNegated(expr,false) )
         throw new ParserException(ParserException.ABORT,"'contains' operator is negated, this may not mean what you think it means, so it's disallowed.");
      // If this is a view select, check whether all selected terms
      // are storable.
      if ( ! validViewSelectTables(stmt) )
         throw new ParserException(ParserException.ABORT,"view selects can not contain non-storable terms "+
               "(ie. interface types and abstract objects as selected terms).");
      // Generate all the specified terms the query will use. Note that
      // leftterms (neither super-,sub-classes nor referenced terms)
      // Note: at this point, the tables have no aliases yet, so keep'em
      // in list not set.
      List allTableTerms = new ArrayList();
      Iterator entryIterator = symbolTable.values().iterator();
      while ( entryIterator.hasNext() )
      {
         SymbolTableEntry entry = (SymbolTableEntry) entryIterator.next();
         SpecifiedTableTerm term = entry.specifiedTerm;
         if ( ! entry.leftTerm )
            allTableTerms.add(term);
      }
      // Generate aliases and connector expressions
      stmt.setQueryExpression(fixAutomaticTerms(expr));
      // Fix all non-storable terms that are used in the expression but
      // not selected.
      fixNonstorableTerms(stmt.getSelectTerms(),stmt.getQueryExpression());
      // Now generate all the root queries for this query.
      // View selects can not contain non-storable terms, and find
      // selects can only contain 1 main term, so get the roots for
      // this main term, and iterate over it's roots.
      QueryStatementList stmts = new QueryStatementList();
      SpecifiedTableTerm selectTerm = mainTerm;
      ClassEntry selectEntry = schemaManager.getClassEntry(selectTerm.getTableName());
      List roots = classTracker.getStorableRootClassEntries(selectEntry);
      // Prepare local lists, these will be used later
      Set localAllTableTermsCore = new HashSet(allTableTerms);
      localAllTableTermsCore.remove(selectTerm);
      if ( logger.isDebugEnabled() )
         logger.debug("determined roots for "+selectEntry+": "+roots);
      // Now generate the statements themselves
      for ( int r=0; r<roots.size(); r++ )
      {
         // Clone statement
         QueryStatement subStmt = stmt.deepCopy();
         Set localAllTableTerms = new HashSet(localAllTableTermsCore);
         // The counter determines with which root to replace select term with
         ClassEntry rootEntry = (ClassEntry) roots.get(r);
         if ( logger.isDebugEnabled() )
            logger.debug("entry "+selectEntry+" is to be replaced with root entry: "+rootEntry);
         ClassInfo rootInfo = classTracker.getClassInfo(rootEntry);
         SpecifiedTableTerm rootTerm = new SpecifiedTableTerm(schemaManager.getTableName(rootEntry),
               selectTerm.getAlias());
         rootTerm.setReferencedLeftTerms(selectTerm.getReferencedLeftTerms()); // Keep referenced terms
         fixLeftTableTerms(rootInfo,rootTerm,selectTerm.getRelatedLeftTerms()); // Add left table terms
         if ( logger.isDebugEnabled() )
            logger.debug("entry "+selectEntry+" ("+selectTerm+") is replaced with root entry: "+rootEntry+" ("+rootTerm+")");
         // Do replace operation in statement. This will replace
         // the term everywhere.
         subStmt.replace(selectTerm,rootTerm);
         // The specified tables are stored in the query itself
         localAllTableTerms.add(rootTerm);
         subStmt.setSpecifiedTerms(localAllTableTerms);
         if ( logger.isDebugEnabled() )
            logger.debug("statement will have specified terms: "+localAllTableTerms);
         // Generate static representation (it is important, that this is
         // before date constraints are added
         subStmt.setStaticRepresentation(subStmt.getMode()+" "+
               subStmt.getSelectTerms().toString()+" "+
               subStmt.getSpecifiedTerms().toString()+" "+
               (subStmt.getQueryExpression()!=null?subStmt.getQueryExpression().toString():"")+
               (subStmt.getOrderByList()!=null?subStmt.getOrderByList().toString():""));
         // Generate date constraints
         subStmt.setQueryExpression(
               fixDateConstraints(localAllTableTerms,
                  subStmt.getQueryExpression(), subStmt.getTimeControl()));
         // Debug
         if ( logger.isDebugEnabled() )
            logger.debug("generated statement, selected: "+subStmt.getSelectTerms()+
                  ", specified: "+subStmt.getSpecifiedTerms()+
                  ", expression: "+subStmt.getQueryExpression()+", order by: "+subStmt.getOrderByList());
         // Insert complete query into result query list
         stmts.add(subStmt);
      }
      // Now generate the query which will return all table names
      if ( stmts.size() > 1 )
      {
         // Clone statement
         QueryStatement subStmt = stmt.deepCopy();
         // Set specified terms
         subStmt.getSpecifiedTerms().addAll(localAllTableTermsCore);
         // Generate static representation (it is important, that this is
         // before date constraints are added
         subStmt.setStaticRepresentation(subStmt.getMode()+" "+
               subStmt.getSelectTerms().toString()+" "+
               subStmt.getSpecifiedTerms().toString()+" "+
               (subStmt.getQueryExpression()!=null?subStmt.getQueryExpression().toString():"")+
               (subStmt.getOrderByList()!=null?subStmt.getOrderByList().toString():""));
         // Generate date constraints on all but the main term
         subStmt.setQueryExpression(
               fixDateConstraints(localAllTableTermsCore,
                  subStmt.getQueryExpression(), subStmt.getTimeControl()));
         if ( logger.isDebugEnabled() )
            logger.debug("generated root statement, selected: "+subStmt.getSelectTerms()+
                  ", specified: "+subStmt.getSpecifiedTerms()+
                  ", expression: "+subStmt.getQueryExpression()+", order by: "+subStmt.getOrderByList());
         // Add
         stmts.setRoot(subStmt);
      }
      return stmts;
   }

   public static class SymbolTableEntry
   {
      public static final int TYPE_HANDLED = 1;
      public static final int TYPE_OBJECT = 3;
      public static final int TYPE_PRIMITIVE = 4;
      
      public SpecifiedTableTerm specifiedTerm; 
                                 // The term this entry represents, if any
      public String referenceColumn;
                                 // Used by handled types, definies the
                                 // reference column, if sub-specified.
      public boolean selected;   // Whether this entry will be selected
      public List termList;      // List of terms in which could not
                                 // be resolved yet
      public List allocatedSuperTerms;
                                 // The list of terms of superclasses
                                 // already allocated (used)
      public ClassInfo classInfo;// Class info if symbol is a table
      public boolean automatic;  // Whether symbol should be automatically
                                 // generated
      public boolean leftTerm;   // Whether this term is standalone, or 
                                 // depending on another term
      public Expression expression;
                                 // The expression this entry generated
      public int type = TYPE_OBJECT;

      public SymbolTableEntry()
      {
         termList = new LinkedList();
         allocatedSuperTerms = new LinkedList();
         leftTerm=false;
      }

      public String toString()
      {
         return "[Symbol entry: "+specifiedTerm+", "+type+":"+automatic+":"+leftTerm+"]";
      }
   }
}

