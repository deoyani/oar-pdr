"""
This module manages the preparation of the metadata needed by pre-publication
landing page service.  It uses an SIPBagger to create the NERDm metadata from 
POD metadata provided by MIDAS and assembles it into an exportable form.  
"""
import os, logging, re, json
from collections import Mapping

from .. import PublishSystem
from ...exceptions import ConfigurationException, StateException, SIPDirectoryNotFound
from ...preserv.bagger import MIDASMetadataBagger
from ...preserv.bagit import NISTBag
from ...utils import build_mime_type_map
from ....id import PDRMinter

log = logging.getLogger(PublishSystem().subsystem_abbrev)

class PrePubMetadataService(PublishSystem):
    """
    The class providing the implementation for the pre-publication metadata
    service.

    This service wraps the MIDASMetadataBagger class which examines the MIDAS 
    upload and review directories for data and metadata and prepares the 
    NERDm metadata.  This class then will serve out the final, combined NERDm 
    record, converting (if so configured) the downloadURLs to bypass the 
    data distribution service (as is necessary for the pre-publication data).  

    This class takes a configuration dictionary at construction; the following
    properties are supported:

    :prop working_dir str #req:  an existing directory where working data can
                      can be stored.  
    :prop review_dir  str #req:  an existing directory containing MIDAS review
                      data
    :prop upload_dir  str #req:  an existing directory containing MIDAS upload
                      data
    :prop id_registry_dir str:   a directory to store the minted ID registry.
                      the default is the value of the working directory.
    :prop mimetype_files list of str ([]):   an ordered list of filepaths to 
                      files that map file extensions to default MIME types.  
                      Mappings in the latter files override those in the former 
                      ones.
    :prop id_minter dict ({}):  a dictionary for configuring the ID minter 
                      instance.
    :prop bagger dict ({}):  a dictionary for configuring the SIPBagger instance
                      used to process the SIP (see SIPBagger implementation 
                      documentation for supported sub-properties).  
    """

    def __init__(self, config, workdir=None, reviewdir=None, uploaddir=None,
                 idregdir=None):
        """
        initialize the service.

        :param config   dict:  the configuration parameters for this service
        :param workdir   str:  the path to the workspace directory where this
                               service will write its data.  If not provided,
                               the value of the 'working_dir' configuration 
                               parameter will be used.
        :param reviewdir str:  the path to the MIDAS-managed directory for SIPs 
                               in the review state.  If not provided,
                               the value of the 'review_dir' configuration 
                               parameter will be used.
        :param uploaddir str:  the path to the MIDAS-managed directory for SIPs
                               in the upload state.  If not provided,
                               the value of the 'upload_dir' configuration 
                               parameter will be used.
        """
        if not isinstance(config, Mapping):
            raise ValueError("PrePubMetadataService: config argument not a " +
                             "dictionary: " + str(config))
        self.cfg = config

        self.log = log.getChild("mdserv")
        
        if not workdir:
            workdir = self.cfg.get('working_dir')
        if not workdir:
            raise ConfigurationException("Missing required config parameters: "+
                                         "working_dir", sys=self)
        if not os.path.isdir(workdir):
            raise StateException("Working directory does not exist as a " +
                                 "directory: " + workdir, sys=self)
        self.workdir = workdir

        if not reviewdir:
            reviewdir = self.cfg.get('review_dir')
        if not reviewdir:
            raise ConfigurationException("Missing required config parameters: "+
                                         "review_dir", sys=self)
        if not os.path.isdir(reviewdir):
            raise StateException("MIDAS review directory does not exist as a " +
                                 "directory: " + reviewdir, sys=self)
        self.reviewdir = reviewdir

        if not uploaddir:
            uploaddir = self.cfg.get('upload_dir')
        if not uploaddir:
            raise ConfigurationException("Missing required config parameters: "+
                                         "upload_dir", sys=self)
        if not os.path.isdir(uploaddir):
            raise StateException("MIDAS Upload directory does not exist as a " +
                                 "directory: " + uploaddir, sys=self)
        self.uploaddir = uploaddir

        if not idregdir:
            idregdir = self.cfg.get('id_registry_dir', self.workdir)
        if not os.path.isdir(idregdir):
            raise StateException("ID Registry directory does not exist as a " +
                                 "directory: " + idregdir, sys=self)

        self._minter = self._create_minter(idregdir)

        mimefiles = self.cfg.get('mimetype_files', [])
        if not isinstance(mimefiles, list):
            mimefiles = [mimefiles]
        self.mimetypes = build_mime_type_map(mimefiles)

    def _create_minter(self, parentdir):
        cfg = self.cfg.get('id_minter', {})
        out = PDRMinter(parentdir, cfg)
        if not os.path.exists(out.registry.store):
            self.log.warn("Creating new ID minter")
        return out

    def prepare_metadata_bag(self, id):
        """
        Bag up the metadata from data provided by MIDAS for a given MIDAS ID.  
        """
        cfg = self.cfg.get('bagger', {})
        if not os.path.exists(self.workdir):
            os.mkdir(workdir)
        elif not os.path.isdir(self.workdir):
            raise StateException("Working directory path not a directory: " +
                                 self.workdir)
        bagger = MIDASMetadataBagger(id, self.workdir, self.reviewdir,
                                     self.uploaddir, cfg, self._minter)
        bagger.ensure_preparation()
        return bagger

    def make_nerdm_record(self, bagdir, datafiles=None, baseurl=None):
        """
        Given a metadata bag, generate a complete NERDm resource record.  

        This may convert all downloadURLs that go through the data distribution 
        service (i.e. that match that service's base URL) to URLs that go
        through a different server.  This is needed for as-yet unreleased data
        as this service is intended to serve.  Conversion is done either by 
        setting the 'download_base_url' parameter in the configuration or by
        providing a baseurl argument.  The value in both cases is the base URL
        to convert the download URLs to.  The config parameter, 
        'datadist_base_path', indicates the base URL path to look for to 
        recognize data distribution service URLs.  If datafiles is also 
        provided, substitution is restricted to those data files given in 
        that lookup map.

        :param bagdir str:  the directory representing the output bag to serve
                            the metadata from 
        :param datafiles str:  a mapping of filepath property values to 
                            locations to existing data files on disk; 
                            substitution is done for filepaths that match
                            one of the paths in the dictionary.  If None,
                            this requirement is not applied.  
        :param baseurl str: the baseurl to convert downloadURLs to; if None,
                            conversion will not be applied unless 
                            'download_base_url' is set (see above).  
        """
        bag = NISTBag(bagdir)
        out = bag.nerdm_record()

        if not baseurl:
            baseurl = self.cfg.get('download_base_url')
        if baseurl and 'components' in out:
            ddspath = self.cfg.get('datadist_base_url', '/od/ds/')
            if ddspath[0] != '/':
                ddspath = '/' + ddspath
            pat = re.compile(r'https?://[\w\.]+(:\d+)?'+ddspath)
            for comp in out['components']:
                # do a download URL substitution if 1) it looks like a
                # distribution service URL, and 2) the file exists in our
                # SIP areas.  
                if 'downloadURL' in comp and pat.search(comp['downloadURL']):
                    # it matches
                    filepath = comp.get('filepath',
                                        pat.sub('',comp['downloadURL']))
                    if datafiles is None or filepath in datafiles:
                        # it exists
                        comp['downloadURL'] = pat.sub(baseurl,
                                                      comp['downloadURL'])

        return out

    def resolve_id(self, id):
        """
        return a full NERDm resource record corresponding to the given 
        MIDAS ID.  
        """
        bagger = self.prepare_metadata_bag(id)
        return self.make_nerdm_record(bagger.bagdir, bagger.datafiles)

    def locate_data_file(self, id, filepath):
        """
        return the location and recommended MIME-type for a data file associated
        with the dataset of a given ID.

        :param id       str:   the dataset's identifier
        :param filepath str:   the relative path to the data file within the 
                                 dataset
        :return tuple:  2-element tuple giving the full filepath and recommended
                                 MIME-type
        """
        bagger = self.prepare_metadata_bag(id)
        if filepath not in bagger.datafiles:
            return (None, None)

        loc = bagger.datafiles[filepath]

        # determine the MIME type to send data as
        bag = NISTBag(bagger.bagdir, True)
        dfmd = bag.nerd_metadata_for(filepath)
        if 'mediaType' in dfmd and dfmd['mediaType']:
            mt = str(dfmd['mediaType'])
        else:
            mt = self.mimetypes.get(os.path.splitext(loc)[1][1:],
                                    'application/octet-stream')
        return (loc, mt)
        
        
