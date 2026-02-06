import React, { useState, useEffect, useCallback } from 'react';
import { Users, UserPlus, Search, Edit2, Trash2, Heart, Filter, RefreshCw, Shield, LogOut, Database } from 'lucide-react';
import './App.css';

const API_BASE = 'http://localhost:8000/api';

export default function ProfileMatcherApp() {
  // --- STATE: SCENE MANAGEMENT ---
  const [scene, setScene] = useState('login'); // 'login', 'user', 'admin'
  const [userRole, setUserRole] = useState('guest');

  // --- EXISTING STATE ---
  const [profiles, setProfiles] = useState([]);
  const [interests, setInterests] = useState([]);
  const [activeTab, setActiveTab] = useState('browse');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  
  const [newProfile, setNewProfile] = useState({ username: '', age: '', interest: '' });
  const [searchQuery, setSearchQuery] = useState('');
  const [matchFilters, setMatchFilters] = useState({ minAge: '', maxAge: '', username: '' });
  const [editMode, setEditMode] = useState(null);
  const [newUsername, setNewUsername] = useState('');

  const showMessage = useCallback((text, type = 'success') => {
      setMessage({ text, type });
      setTimeout(() => setMessage(''), 3000);
  }, []);

  const loadProfiles = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/profiles`);
      const data = await res.json();
      setProfiles(data);
    } catch (err) {
      showMessage('Failed to load profiles', 'error');
    }
    setLoading(false);
  }, [showMessage]);

  const loadInterests = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/interests`);
      const data = await res.json();
      setInterests(data);
    } catch (err) {
      console.error('Failed to load interests');
    }
  }, []);

  // Sync role with backend
  const switchBackendRole = async (role) => {
    try {
      await fetch(`${API_BASE}/admin/role`, {
        method: 'POST',
        body: JSON.stringify({ role: role })
      });
      setUserRole(role);
      showMessage(`Switched database to ${role} mode`);
    } catch (e) {
      showMessage('Failed to switch DB role', 'error');
    }
  };

  useEffect(() => {
    if (scene !== 'login') {
        loadProfiles();
        loadInterests();
    }
  }, [scene, loadProfiles, loadInterests]);

  // --- NEW: AGE VALIDATION HELPER ---
  const validateAgeInput = (value) => {
    // Allow empty string (so user can delete)
    if (value === '') return true;
    // Check if number and within range
    const num = parseInt(value);
    return !isNaN(num) && num >= 0 && num <= 150;
  };

  const handleAgeChange = (e, setter, currentObj, field = 'age') => {
    const val = e.target.value;
    if (validateAgeInput(val)) {
      setter({ ...currentObj, [field]: val });
    }
  };

  // --- HANDLERS (Existing) ---
  const handleCreateProfile = async () => {
    if (!newProfile.username || !newProfile.age || !newProfile.interest) {
      showMessage('Please fill all fields', 'error');
      return;
    }

    // NEW: Final Logic Check before submission
    const ageNum = parseInt(newProfile.age);
    if (ageNum < 0 || ageNum > 150) {
        showMessage('Age must be between 0 and 150', 'error');
        return;
    }

    try {
      const res = await fetch(`${API_BASE}/profiles`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newProfile)
      });
      if (res.ok) {
        showMessage('Profile created successfully!');
        setNewProfile({ username: '', age: '', interest: '' });
        loadProfiles();
      } else { showMessage('Failed to create', 'error'); }
    } catch (err) { showMessage('Error creating profile', 'error'); }
  };

  const handleRename = async (currentName) => {
    if (!newUsername.trim()) return;
    try {
      const res = await fetch(`${API_BASE}/profiles?currentName=${currentName}&newName=${newUsername}`, { method: 'PUT' });
      if (res.ok) { showMessage('Renamed successfully!'); setEditMode(null); loadProfiles(); }
    } catch (err) { showMessage('Error renaming', 'error'); }
  };

  const handleDelete = async (username) => {
    if (!window.confirm(`Delete ${username}?`)) return;
    try {
      const res = await fetch(`${API_BASE}/profiles?username=${username}`, { method: 'DELETE' });
      if (res.ok) { showMessage('Deleted successfully!'); loadProfiles(); }
    } catch (err) { showMessage('Error deleting', 'error'); }
  };

  const getMatches = () => {
    if (!matchFilters.minAge || !matchFilters.maxAge) return [];
    const min = parseInt(matchFilters.minAge);
    const max = parseInt(matchFilters.maxAge);
    return profiles.filter(p => p.username !== matchFilters.username && p.age >= min && p.age <= max);
  };

  const filteredProfiles = profiles.filter(p =>
    p.username.toLowerCase().includes(searchQuery.toLowerCase()) ||
    p.interest.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // --- SCENE 1: LOGIN / LANDING ---
  if (scene === 'login') {
    return (
      <div className="min-h-screen bg-gradient-to-br from-purple-600 to-blue-600 flex items-center justify-center p-4">
        <div className="bg-white rounded-2xl shadow-xl p-8 max-w-md w-full text-center space-y-8">
            <div className="flex justify-center">
                <div className="bg-purple-100 p-4 rounded-full">
                    <Heart className="w-12 h-12 text-purple-600" />
                </div>
            </div>
            <div>
                <h1 className="text-3xl font-bold text-gray-900">Welcome</h1>
                <p className="text-gray-500 mt-2">Select your access level</p>
            </div>
            
            <div className="space-y-4">
                <button onClick={() => { setScene('user'); switchBackendRole('guest'); }} 
                    className="w-full py-4 bg-gray-100 hover:bg-gray-200 rounded-xl flex items-center justify-center gap-3 transition group">
                    <Users className="w-6 h-6 text-gray-600 group-hover:text-gray-900" />
                    <span className="text-lg font-medium text-gray-700">Enter as User</span>
                </button>
                <div className="relative">
                    <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-gray-200"></div></div>
                    <div className="relative flex justify-center text-sm"><span className="px-2 bg-white text-gray-500">Staff Only</span></div>
                </div>
                <button onClick={() => { setScene('admin'); switchBackendRole('admin'); }} 
                    className="w-full py-4 bg-gray-900 hover:bg-gray-800 text-white rounded-xl flex items-center justify-center gap-3 transition shadow-lg">
                    <Shield className="w-6 h-6" />
                    <span className="text-lg font-medium">Admin Dashboard</span>
                </button>
            </div>
        </div>
      </div>
    );
  }

  // --- SHARED HEADER FOR SCENES ---
  return (
    <div className="min-h-screen bg-gray-50">
      <header className={`shadow-sm border-b ${scene === 'admin' ? 'bg-gray-900 border-gray-800' : 'bg-white border-gray-200'}`}>
        <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className={`p-2 rounded-lg ${scene === 'admin' ? 'bg-gray-800' : 'bg-purple-100'}`}>
               {scene === 'admin' ? <Shield className="w-6 h-6 text-white" /> : <Heart className="w-6 h-6 text-purple-600" />}
            </div>
            <h1 className={`text-xl font-bold ${scene === 'admin' ? 'text-white' : 'text-gray-900'}`}>
                {scene === 'admin' ? 'System Administrator' : 'Find Friends'}
            </h1>
          </div>
          <button onClick={() => setScene('login')} className="text-sm font-medium hover:underline opacity-70 hover:opacity-100 flex items-center gap-2">
            <LogOut className="w-4 h-4" />
            <span className={scene === 'admin' ? 'text-black' : 'text-gray-900'}>Logout</span>
          </button>
        </div>
      </header>

      {message && (
        <div className={`fixed top-4 right-4 px-6 py-3 rounded-lg shadow-lg z-50 text-white ${message.type === 'error' ? 'bg-red-500' : 'bg-green-500'}`}>
          {message.text}
        </div>
      )}

      <main className="max-w-7xl mx-auto px-4 py-8">
        
        {/* Loading State Indicator */}
        {loading && (
            <div className="text-center py-4 mb-4">
                <span className="inline-block animate-pulse text-gray-500 font-medium">Updating data...</span>
            </div>
        )}

        {/* --- SCENE 2: ADMIN DASHBOARD --- */}
        {scene === 'admin' ? (
            <div className="space-y-6">
                {/* Stats Row */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
                        <div className="text-gray-500 text-sm font-medium uppercase">Total Profiles</div>
                        <div className="text-3xl font-bold text-gray-900 mt-2">{profiles.length}</div>
                    </div>
                    <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
                        <div className="text-gray-500 text-sm font-medium uppercase">DB Connection</div>
                        <div className="flex items-center gap-2 mt-2 text-green-600 font-bold">
                            <Database className="w-5 h-5" />
                            <span>Active (Role: {userRole})</span>
                        </div>
                    </div>
                    <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
                        <div className="text-gray-500 text-sm font-medium uppercase">System Status</div>
                        <div className="mt-2 inline-block px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm font-medium">Operational</div>
                    </div>
                </div>

                {/* Admin Table */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                    <div className="px-6 py-4 border-b border-gray-200 flex justify-between items-center bg-gray-50">
                        <h2 className="font-semibold text-gray-700">Database Records</h2>
                        <button onClick={loadProfiles} className="p-2 hover:bg-white rounded-lg transition"><RefreshCw className="w-4 h-4" /></button>
                    </div>
                    <table className="w-full text-left">
                        <thead className="bg-gray-50 text-gray-500 text-xs uppercase font-semibold">
                            <tr>
                                <th className="px-6 py-3">Username</th>
                                <th className="px-6 py-3">Age</th>
                                <th className="px-6 py-3">Interest</th>
                                <th className="px-6 py-3 text-right">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200">
                            {profiles.map(p => (
                                <tr key={p.username} className="hover:bg-gray-50">
                                    <td className="px-6 py-4 font-medium text-gray-900">{p.username}</td>
                                    <td className="px-6 py-4">{p.age}</td>
                                    <td className="px-6 py-4"><span className="px-2 py-1 bg-purple-50 text-purple-700 rounded text-xs">{p.interest}</span></td>
                                    <td className="px-6 py-4 text-right">
                                        <button onClick={() => handleDelete(p.username)} className="text-red-600 hover:text-red-900 text-sm font-medium flex items-center justify-end gap-1 ml-auto">
                                            <Trash2 className="w-4 h-4" />
                                            Delete
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        ) : (
            // --- SCENE 3: USER DASHBOARD (Existing code wrapped here) ---
            <>
                <div className="bg-white rounded-xl shadow-sm mb-6 p-1 flex gap-2">
                {[
                    { id: 'browse', label: 'Browse', icon: Users },
                    { id: 'create', label: 'Join', icon: UserPlus },
                    { id: 'match', label: 'Match', icon: Filter }
                ].map(tab => (
                    <button key={tab.id} onClick={() => setActiveTab(tab.id)}
                    className={`flex-1 flex items-center justify-center gap-2 px-4 py-3 rounded-lg transition ${activeTab === tab.id ? 'bg-gradient-to-r from-purple-500 to-blue-500 text-white shadow-md' : 'text-gray-600 hover:bg-gray-50'}`}>
                    <tab.icon className="w-5 h-5" /><span className="font-medium">{tab.label}</span>
                    </button>
                ))}
                </div>

                {activeTab === 'browse' && (
                <div className="space-y-6">
                    <div className="bg-white rounded-xl shadow-sm p-4 relative">
                        <Search className="absolute left-6 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                        <input type="text" placeholder="Search profiles..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)}
                        className="w-full pl-10 pr-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent" />
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {filteredProfiles.map(profile => (
                            <div key={profile.username} className="bg-white rounded-xl shadow-sm hover:shadow-md transition p-6">
                                <div className="flex justify-between items-start mb-4">
                                    {editMode === profile.username ? (
                                        <div className="flex gap-2 w-full">
                                            <input value={newUsername} onChange={e=>setNewUsername(e.target.value)} className="flex-1 border rounded px-2 py-1" />
                                            <button onClick={()=>handleRename(profile.username)} className="bg-green-500 text-white px-3 rounded text-sm">Save</button>
                                        </div>
                                    ) : (
                                        <h3 className="text-xl font-bold">{profile.username}</h3>
                                    )}
                                    {editMode !== profile.username && (
                                        <div className="flex gap-1">
                                            <button onClick={()=>{setEditMode(profile.username); setNewUsername(profile.username)}} className="p-2 hover:bg-gray-100 rounded"><Edit2 className="w-4 h-4 text-gray-500"/></button>
                                        </div>
                                    )}
                                </div>
                                <div className="space-y-1 text-sm text-gray-600">
                                    <p>Age: <span className="text-gray-900 font-medium">{profile.age}</span></p>
                                    <p>Interest: <span className="text-purple-600 font-medium">{profile.interest}</span></p>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
                )}

                {activeTab === 'create' && (
                    <div className="max-w-2xl mx-auto bg-white rounded-xl shadow-sm p-8 space-y-4">
                        <h2 className="text-2xl font-bold">Create Profile</h2>
                        <input type="text" placeholder="Username" value={newProfile.username} onChange={e=>setNewProfile({...newProfile, username: e.target.value})} className="w-full p-3 border rounded-lg"/>
                        
                        {/* UPDATE: Added validation to onChange */}
                        <input 
                            type="number" 
                            placeholder="Age" 
                            value={newProfile.age} 
                            onChange={(e) => handleAgeChange(e, setNewProfile, newProfile, 'age')} 
                            className="w-full p-3 border rounded-lg"
                        />
                        
                        <select value={newProfile.interest} onChange={e=>setNewProfile({...newProfile, interest: e.target.value})} className="w-full p-3 border rounded-lg">
                            <option value="">Select Interest</option>
                            {interests.map(i=><option key={i} value={i}>{i}</option>)}
                        </select>
                        <button onClick={handleCreateProfile} className="w-full py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700">Join Now</button>
                    </div>
                )}

                {activeTab === 'match' && (
                    <div className="space-y-6">
                        <div className="bg-white rounded-xl shadow-sm p-6 grid grid-cols-1 md:grid-cols-3 gap-4">
                            <input placeholder="Your Username" value={matchFilters.username} onChange={e=>setMatchFilters({...matchFilters, username: e.target.value})} className="p-3 border rounded-lg" />
                            
                            {/* UPDATE: Added validation to Match Filters too */}
                            <input 
                                placeholder="Min Age" 
                                type="number" 
                                value={matchFilters.minAge} 
                                onChange={(e) => handleAgeChange(e, setMatchFilters, matchFilters, 'minAge')} 
                                className="p-3 border rounded-lg" 
                            />
                            <input 
                                placeholder="Max Age" 
                                type="number" 
                                value={matchFilters.maxAge} 
                                onChange={(e) => handleAgeChange(e, setMatchFilters, matchFilters, 'maxAge')} 
                                className="p-3 border rounded-lg" 
                            />
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                            {getMatches().map(p => (
                                <div key={p.username} className="bg-white p-6 rounded-xl border-2 border-purple-100">
                                    <div className="font-bold text-lg mb-2">{p.username}</div>
                                    <div className="text-sm text-gray-500">Age: {p.age} • Likes {p.interest}</div>
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </>
        )}
      </main>
    </div>
  );
}