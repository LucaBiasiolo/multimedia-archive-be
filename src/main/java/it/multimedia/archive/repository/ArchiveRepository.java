package it.multimedia.archive.repository;

import it.multimedia.archive.Archive;

import java.util.List;

public interface ArchiveRepository {

    List getArchives();

    Archive insertArchive(Archive archive);
}
