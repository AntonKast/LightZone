#
# spec file for package lightzone
#

Name:           lightzone
Version:	3.9.1
Release:	7
License:	GPLv2+
Summary:	Open-source digital darkroom software
Url:		http://lightzoneproject.org/
Group:		Productivity/Graphics/Convertors 
Source:		%{name}-%{version}.tar.gz
#Patch:
BuildRequires:	ant, autoconf, automake, nasm, gcc, gcc-c++, libtool, make, tidy, git, javahelp2

%if 0%{?fedora}
BuildRequires: java-1.7.0-openjdk-devel, libX11-devel, xz-libs
%define debug_package %{nil}
%endif
%if 0%{?sles_version}
BuildRequires: java-1_6_0-openjdk-devel, xorg-x11-libX11-devel, liblzma5
%endif
%if 0%{?suse_version} == 1210
BuildRequires: java-1_6_0-openjdk-devel, xorg-x11-libX11-devel, liblzma5
%endif
%if 0%{?suse_version} > 1210
BuildRequires: java-1_7_0-openjdk-devel, libX11-devel, liblzma5
%endif
%if 0%{?centos_version}
BuildRequires: java-1.6.0-openjdk-devel, libX11-devel, liblzma5
%define debug_package %{nil}
%endif
%if 0%{?mdkversion}
BuildRequires: java-1.6.0-openjdk-devel, libX11-devel, liblzma5
%endif

Requires:	java >= 1.6.0
Obsoletes:	lightzone < %{version}
Provides:	lightzone = %{version}
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
Packager:	Andreas Rother
#Prefix:	/opt
#BuildArch:	noarch
%description
LightZone is professional-level digital darkroom software for Windows, Mac OS X, and Linux. Rather than using layers as many other photo editors do, LightZone lets the user build up a stack of tools which can be rearranged, turned off and on, and removed from the stack. It's a non-destructive editor, where any of the tools can be re-adjusted or modified later — even in a different editing session. A tool stack can be copied to a batch of photos at one time. LightZone operates in a 16-bit linear color space with the wide gamut of ProPhoto RGB.

%prep
%setup -q

%build
%ant -f linux/build.xml jar

%install
%if 0%{?sles_version}
export NO_BRP_CHECK_BYTECODE_VERSION=true
%endif

%define instdir /opt/%{name}
install -dm 0755 "%buildroot/%{instdir}"
cp -rpH lightcrafts/products/dcraw "%buildroot/%{instdir}"
cp -rpH lightcrafts/products/LightZone-forkd "%buildroot/%{instdir}"
cp -rpH linux/products/*.so "%buildroot/%{instdir}"
cp -rpH linux/products/*.jar "%buildroot/%{instdir}"

#startscript
cat > %{name} << 'EOF'
#!/bin/sh
#
# LightZone startscript
#
echo Starting %{name} version %{version} ...
echo with options : ${@}

totalmem=`cat /proc/meminfo | grep MemTotal | sed -r 's/.* ([0-9]+) .*/\1/'`
if [ $totalmem -ge 1024000 ]; then
        maxmem=$(( $totalmem / 2 ))
else
        maxmem=512000
fi

(cd "%{instdir}" && LD_LIBRARY_PATH="%{instdir}" exec java -Xmx${maxmem}k -Djava.library.path="%{instdir}" -Dfile.encoding=UTF8 -classpath "%{instdir}/*" com.lightcrafts.platform.linux.LinuxLauncher ${@} )
EOF
install -d -m 755 %{buildroot}%{_bindir}
install -m 755 %{name} %{buildroot}%{_bindir}/

%post

%postun

%files
%defattr(-,root,root)
%doc COPYING README.md linux/BUILD-Linux.md
%{instdir}
%_bindir/%name

%changelog

