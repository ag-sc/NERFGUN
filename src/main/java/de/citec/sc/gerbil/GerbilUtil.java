package de.citec.sc.gerbil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentCreator;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.aksw.gerbil.transfer.nif.data.SpanImpl;

import com.google.gson.Gson;

import de.citec.sc.corpus.Annotation;

public class GerbilUtil {

	public static final String DBPEDIA_LINK_PREFIX = "http://dbpedia.org/resource/";

	// public static de.citec.sc.corpus.Document
	// gerbilDocument2BireDocument(Document gerbilDocument) {
	// de.citec.sc.corpus.Document document = new
	// de.citec.sc.corpus.Document(gerbilDocument.getText(),
	// gerbilDocument.getDocumentURI());
	// List<Annotation> annotations =
	// gerbilDocument.getMarkings(SpanImpl.class).stream()
	// .map(span -> GerbilUtil.gerbil2bire(span,
	// gerbilDocument.getText())).collect(Collectors.toList());
	// document.setGoldStandard(annotations);
	// return document;
	// }
	//
	// public static Document
	// bireDocument2GerbilDocument(de.citec.sc.corpus.Document bireDocument) {
	// Document document = new DocumentImpl(bireDocument.getDocumentContent(),
	// bireDocument.getDocumentName());
	// List<Marking> annotations = bireDocument.getAnnotations().stream()
	// .map(annotation ->
	// GerbilUtil.bire2gerbil(annotation)).collect(Collectors.toList());
	// document.setMarkings(annotations);
	// return document;
	// }
	//
	// public static List<Marking> convertAnnotations(List<Annotation>
	// annotations) {
	// List<Marking> gerbilAnnotations = new ArrayList<>();
	// for (Annotation annotation : annotations) {
	// int start = annotation.getStartIndex();
	// int length = annotation.getEndIndex() - annotation.getStartIndex();
	// String uri = DBPEDIA_LINK_PREFIX + annotation.getLink();
	// NamedEntity gerbilAnnotation = new NamedEntity(start, length, uri);
	// gerbilAnnotations.add(gerbilAnnotation);
	// }
	// return gerbilAnnotations;
	// }
	//
	// public static NamedEntity bire2gerbil(Annotation annotation) {
	// int start = annotation.getStartIndex();
	// int length = annotation.getEndIndex() - annotation.getStartIndex();
	// String uri = DBPEDIA_LINK_PREFIX + annotation.getLink();
	// NamedEntity gerbilAnnotation = new NamedEntity(start, length, uri);
	// return gerbilAnnotation;
	// }
	//
	// public static Annotation gerbil2bire(SpanImpl span, String documentText)
	// {
	// int start = span.getStartPosition();
	// int end = start + span.getLength();
	// String text = documentText.substring(start, end);
	// Annotation annotation = new Annotation(text, "", start, end);
	// return annotation;
	// }

	public static Annotation gson2bire(GsonAnnotation a) {
		Annotation annotation = new Annotation(a.text, a.uri, a.start, a.end);
		return annotation;
	}

	public static GsonAnnotation bire2gson(Annotation a) {
		GsonAnnotation annotation = new GsonAnnotation();
		annotation.start = a.getStartIndex();
		annotation.end = a.getEndIndex();
		annotation.text = a.getWord();
		annotation.uri = a.getLink();
		return annotation;
	}

	public static NamedEntity gson2gerbil(GsonAnnotation annotation) {
		int start = annotation.start;
		int length = annotation.end - annotation.start;
		String uri = DBPEDIA_LINK_PREFIX + annotation.uri;
		NamedEntity gerbilAnnotation = new NamedEntity(start, length, uri);
		return gerbilAnnotation;
	}

	public static GsonAnnotation gerbil2gson(SpanImpl span, String documentText) {
		GsonAnnotation annotation = new GsonAnnotation();
		annotation.start = span.getStartPosition();
		annotation.end = annotation.start + span.getLength();
		annotation.text = documentText.substring(annotation.start, annotation.end);
		annotation.uri = "";
		return annotation;
	}

	public static GsonDocument gerbil2gson(Document document) {
		GsonDocument gson = new GsonDocument();
		gson.text = document.getText();
		gson.name = document.getDocumentURI();
		gson.annotations = document.getMarkings(SpanImpl.class).stream()
				.map(span -> GerbilUtil.gerbil2gson(span, document.getText())).collect(Collectors.toList());
		return gson;
	}

	public static Document gson2gerbil(GsonDocument gsonDocument) {
		Document doc = new DocumentImpl();
		doc.setText(gsonDocument.text);
		doc.setDocumentURI(gsonDocument.name);
		doc.setMarkings(gsonDocument.annotations.stream().map(a -> GerbilUtil.gson2gerbil(a)).collect(Collectors.toList()));
		return doc;
	}

	public static GsonDocument bire2gson(de.citec.sc.corpus.Document document) {
		GsonDocument gson = new GsonDocument();
		gson.text = document.getDocumentContent();
		gson.name = document.getDocumentName();
		gson.annotations = document.getAnnotations().stream().map(span -> GerbilUtil.bire2gson(span))
				.collect(Collectors.toList());
		return gson;
	}

	public static de.citec.sc.corpus.Document gson2bire(GsonDocument gsonDocument) {
		de.citec.sc.corpus.Document doc = new de.citec.sc.corpus.Document(gsonDocument.text, gsonDocument.name);
		doc.setAnnotations(
				gsonDocument.annotations.stream().map(span -> GerbilUtil.gson2bire(span)).collect(Collectors.toList()));
		return doc;
	}

	public static String gerbil2json(Document document) {
		Gson gson = new Gson();
		return gson.toJson(gerbil2gson(document));
	}

	public static Document json2gerbil(String jsonDocument) {
		Gson gson = new Gson();
		return gson2gerbil(gson.fromJson(jsonDocument, GsonDocument.class));
	}

	public static String bire2json(de.citec.sc.corpus.Document document) {
		Gson gson = new Gson();
		return gson.toJson(bire2gson(document));
	}

	public static de.citec.sc.corpus.Document json2bire(String jsonDocument) {
		Gson gson = new Gson();
		return gson2bire(gson.fromJson(jsonDocument, GsonDocument.class));
	}
}
